package dev.zm.disguise.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Optional hook for LuckPerms.
 * Provides prefixes, suffixes, groups, and transient node management.
 * Loaded only if LuckPerms is installed.
 */
public class LuckPermsHook {

    private boolean enabled;
    private LuckPerms api;

    public void register() {
        try {
            api = LuckPermsProvider.get();
            enabled = true;
        } catch (Exception e) {
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ─── Grupos del jugador ───────────────────────────────────────────────────

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
        return api.getUserManager().loadUser(uuid).thenApply(user -> {
            return user != null ? user.getPrimaryGroup() : "";
        });
    }

    /**
     * Obtiene TODOS los grupos directos (no heredados) que tiene un usuario.
     */
    public CompletableFuture<List<String>> getAllGroupsAsync(UUID uuid) {
        if (!enabled || api == null)
            return CompletableFuture.completedFuture(Collections.emptyList());
        return api.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null)
                return Collections.emptyList();
            List<String> groups = new ArrayList<>();
            for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
                groups.add(node.getGroupName());
            }
            return groups;
        });
    }

    // ─── Transient node manipulation (Disguise) ────────────────────────────────

    /**
     * Clears transient nodes injected by zMDisguise and applies the new ones.
     */
    public void applyTransientGroups(Player player, List<String> groupsToApply) {
        if (!enabled || api == null || groupsToApply == null || groupsToApply.isEmpty())
            return;

        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return;

        // Clear all previous transient InheritanceNodes for safety
        user.transientData().clear(NodeType.INHERITANCE::matches);

        for (String group : groupsToApply) {
            InheritanceNode node = InheritanceNode.builder(group).build();
            user.transientData().add(node);
        }
    }

    /**
     * Removes all transient groups injected when the disguise is removed.
     */
    public void removeTransientGroups(Player player) {
        if (!enabled || api == null)
            return;

        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return;

        user.transientData().clear(NodeType.INHERITANCE::matches);
    }

    // ─── Group prefix / suffix ─────────────────────────────────────────────────

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

    // ─── All loaded groups ────────────────────────────────────────────────────

    public Collection<Group> getAllGroups() {
        if (!enabled || api == null)
            return Collections.emptyList();
        return api.getGroupManager().getLoadedGroups();
    }
}
