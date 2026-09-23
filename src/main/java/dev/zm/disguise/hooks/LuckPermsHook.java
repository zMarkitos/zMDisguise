package dev.zm.disguise.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.event.user.UserLoadEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LuckPerms integration.
 *
 * <p>Disguising swaps the player's <em>real</em> group membership for the target's groups so that
 * every plugin reading LuckPerms (TAB, chat, scoreboards, placeholders) shows the disguised rank.
 * Only adding transient groups on top never worked: the real group (usually the heaviest one)
 * kept winning the prefix and primary-group resolution.
 *
 * <p>Real groups are removed in memory only (never saved), the target groups are added as transient
 * nodes, and optionally the player's effective permissions are snapshotted into transient nodes so
 * they keep their powers while disguised. Undisguising re-adds the real groups and saves the user,
 * which also repairs the case where another plugin persisted the user while disguised.
 */
public class LuckPermsHook {

    private boolean enabled;
    private LuckPerms api;

    /** Transient nodes we injected, per player, so we remove exactly those and nothing else. */
    private final Map<UUID, List<Node>> injected = new ConcurrentHashMap<>();
    /** The real inheritance nodes we detached, per player (kept with contexts for an exact restore). */
    private final Map<UUID, List<InheritanceNode>> detached = new ConcurrentHashMap<>();

    public void register(Object plugin) {
        try {
            api = LuckPermsProvider.get();
            enabled = true;
            api.getEventBus().subscribe(plugin, UserLoadEvent.class, this::onUserLoad);
        } catch (Exception e) {
            enabled = false;
        }
    }

    private void onUserLoad(UserLoadEvent event) {
        User user = event.getUser();
        List<InheritanceNode> nodes = detached.get(user.getUniqueId());
        if (nodes == null)
            return;
        for (InheritanceNode n : nodes) {
            user.data().remove(n);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Player groups

    public String getPrimaryGroup(Player player) {
        if (!enabled || api == null)
            return "";
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return "";
        return user.getPrimaryGroup();
    }

    public CompletableFuture<String> getPrimaryGroupAsync(UUID uuid) {
        if (!enabled || api == null)
            return CompletableFuture.completedFuture("");
        return api.getUserManager().loadUser(uuid).thenApply(user -> user != null ? user.getPrimaryGroup() : "");
    }

    /** All direct (non-inherited, persistent) groups of a user. */
    public CompletableFuture<List<String>> getAllGroupsAsync(UUID uuid) {
        if (!enabled || api == null)
            return CompletableFuture.completedFuture(Collections.emptyList());
        return api.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null)
                return Collections.emptyList();
            List<String> groups = new ArrayList<>();
            for (Node n : user.data().toCollection()) {
                if (NodeType.INHERITANCE.matches(n)) {
                    groups.add(NodeType.INHERITANCE.cast(n).getGroupName());
                }
            }
            return groups;
        });
    }

    // Disguise group swap

    /**
     * Detaches the player's real groups and attaches the disguise groups.
     *
     * @param knownReal real groups recorded earlier (e.g. loaded from storage after a crash); merged in
     * @return the real group names that were detached, to be stored for restoration
     */
    public List<String> applyDisguiseGroups(Player player, List<String> targetGroups, List<String> knownReal,
            boolean keepPermissions) {
        List<String> fallback = knownReal == null ? Collections.emptyList() : knownReal;
        if (!enabled || api == null)
            return fallback;
        UUID uuid = player.getUniqueId();
        User user = api.getUserManager().getUser(uuid);
        if (user == null)
            return fallback;

        // A second /disguise while already disguised: put the real groups back first so the
        // snapshot below reflects the real player, not the previous disguise.
        if (hasDetached(uuid)) {
            reattachInMemory(user, uuid, knownReal);
        }

        Map<String, Boolean> permissionSnapshot = keepPermissions
                ? user.getCachedData().getPermissionData().getPermissionMap()
                : Collections.emptyMap();

        List<InheritanceNode> realNodes = new ArrayList<>();
        Set<String> realNames = new LinkedHashSet<>();
        if (knownReal != null)
            realNames.addAll(knownReal);
        for (Node n : user.data().toCollection()) {
            if (NodeType.INHERITANCE.matches(n)) {
                InheritanceNode in = NodeType.INHERITANCE.cast(n);
                realNodes.add(in);
                realNames.add(in.getGroupName());
            }
        }
        // Groups only known from storage (not currently on the user) are rebuilt without contexts.
        for (String name : realNames) {
            if (realNodes.stream().noneMatch(n -> n.getGroupName().equalsIgnoreCase(name))) {
                realNodes.add(InheritanceNode.builder(name).build());
            }
        }

        for (InheritanceNode n : realNodes) {
            user.data().remove(n);
        }
        detached.put(uuid, realNodes);

        List<Node> added = new ArrayList<>();
        List<String> targets = (targetGroups == null || targetGroups.isEmpty()) ? List.of("default") : targetGroups;
        for (String group : targets) {
            Node n = InheritanceNode.builder(group).build();
            user.transientData().add(n);
            added.add(n);
        }
        for (Map.Entry<String, Boolean> e : permissionSnapshot.entrySet()) {
            String key = e.getKey();
            if (!Boolean.TRUE.equals(e.getValue()))
                continue;
            Node n = Node.builder(key).value(true).build();
            if (!NodeType.PERMISSION.matches(n))
                continue; // group./prefix./suffix./meta./weight. keys would leak the real rank back in
            user.transientData().add(n);
            added.add(n);
        }
        injected.put(uuid, added);
        return new ArrayList<>(realNames);
    }

    /**
     * Removes the disguise groups and re-attaches the real groups. Saves the user so a persisted
     * disguised state (another plugin saving the user mid-disguise) is repaired as well.
     */
    public void restoreRealGroups(UUID uuid, List<String> knownReal) {
        if (!enabled || api == null)
            return;
        User user = api.getUserManager().getUser(uuid);
        if (user != null) {
            reattachInMemory(user, uuid, knownReal);
            api.getUserManager().saveUser(user);
            return;
        }
        // Player already gone (restore after a crash for an offline user)
        List<String> real = knownReal == null ? List.of() : new ArrayList<>(knownReal);
        if (real.isEmpty())
            return;
        api.getUserManager().modifyUser(uuid, u -> {
            for (String g : real)
                u.data().add(InheritanceNode.builder(g).build());
        });
    }

    private void reattachInMemory(User user, UUID uuid, List<String> knownReal) {
        List<Node> ours = injected.remove(uuid);
        if (ours != null) {
            for (Node n : ours)
                user.transientData().remove(n);
        }
        List<InheritanceNode> nodes = detached.remove(uuid);
        Set<String> present = new LinkedHashSet<>();
        if (nodes != null) {
            for (InheritanceNode n : nodes) {
                user.data().add(n);
                present.add(n.getGroupName().toLowerCase());
            }
        }
        if (knownReal != null) {
            for (String g : knownReal) {
                if (present.add(g.toLowerCase()))
                    user.data().add(InheritanceNode.builder(g).build());
            }
        }
    }

    /** True if this hook currently holds detached groups for the player. */
    public boolean hasDetached(UUID uuid) {
        return detached.containsKey(uuid) || injected.containsKey(uuid);
    }

    // Group prefix / suffix

    public String getPrefix(String groupName) {
        if (!enabled || api == null || groupName == null)
            return "";
        Group group = api.getGroupManager().getGroup(groupName);
        if (group == null)
            return "";
        String prefix = group.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }

    public String getSuffix(String groupName) {
        if (!enabled || api == null || groupName == null)
            return "";
        Group group = api.getGroupManager().getGroup(groupName);
        if (group == null)
            return "";
        String suffix = group.getCachedData().getMetaData().getSuffix();
        return suffix != null ? suffix : "";
    }

    public Collection<Group> getAllGroups() {
        if (!enabled || api == null)
            return Collections.emptyList();
        return api.getGroupManager().getLoadedGroups();
    }
}
