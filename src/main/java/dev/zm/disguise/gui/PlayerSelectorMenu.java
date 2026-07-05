package dev.zm.disguise.gui;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.config.SettingsManager;
import dev.zm.disguise.hooks.LuckPermsHook;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Paginated menu showing all players (online and offline).
 * Clicking on a player fully copies their visual identity.
 * Data is loaded asynchronously to avoid lag.
 */
public class PlayerSelectorMenu implements Menu, Listener {

    private final zMDisguise plugin;
    private final Player viewer;
    private final List<OfflinePlayer> players = new ArrayList<>();
    private int page = 0;
    private static final int PAGE_SIZE = 45;

    private Inventory inventory;
    private boolean isBuildingPage = false;

    public PlayerSelectorMenu(zMDisguise plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** Starts async player loading and opens the menu when done. */
    public void openAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            java.util.Set<java.util.UUID> uuids = new java.util.HashSet<>();
            List<OfflinePlayer> all = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(viewer)) {
                    uuids.add(p.getUniqueId());
                    all.add(p);
                }
            }
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && !uuids.contains(op.getUniqueId())
                        && !op.getUniqueId().equals(viewer.getUniqueId())) {
                    uuids.add(op.getUniqueId());
                    all.add(op);
                }
            }

            // Sort by last played (most recent first)
            all.sort(Comparator.comparingLong(OfflinePlayer::getLastPlayed).reversed());

            this.players.addAll(all);

            Bukkit.getScheduler().runTask(plugin, () -> {
                open(viewer);
            });
        });
    }

    @Override
    public Inventory getInventory() {
        SettingsManager cfg = plugin.getSettingsManager();
        String rawTitle = cfg.getMenuTitle("player-selector");
        Component title = MessageManager.colorize(rawTitle);

        inventory = Bukkit.createInventory(null, 54, title);
        buildPage();
        return inventory;
    }

    private void buildPage() {
        if (isBuildingPage)
            return;
        isBuildingPage = true;
        inventory.clear();

        // Show a loading item while resolving async
        ItemStack loading = buildSimpleItem(Material.CLOCK, "&#FFD700Loading...");
        for (int i = 0; i < PAGE_SIZE; i++)
            inventory.setItem(i, loading);

        // Bottom bar
        ItemStack filler = buildSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, filler);
        if (page > 0) {
            inventory.setItem(45,
                    buildSimpleItem(Material.ARROW, plugin.getMessageManager().getString("gui-items.previous")));
        }
        int end = Math.min((page + 1) * PAGE_SIZE, players.size());
        if (end < players.size()) {
            inventory.setItem(53,
                    buildSimpleItem(Material.ARROW, plugin.getMessageManager().getString("gui-items.next")));
        }
        inventory.setItem(49,
                buildSimpleItem(Material.BARRIER, plugin.getMessageManager().getString("gui-items.back")));

        // Load skulls asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int start = page * PAGE_SIZE;
            List<ItemStack> skulls = new ArrayList<>();
            for (int i = start; i < end; i++) {
                skulls.add(buildSkullSyncOrAsync(players.get(i)));
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < skulls.size(); i++) {
                    inventory.setItem(i, skulls.get(i));
                }
                // Clear remaining slots if fewer than PAGE_SIZE
                for (int i = skulls.size(); i < PAGE_SIZE; i++) {
                    inventory.setItem(i, new ItemStack(Material.AIR));
                }
                isBuildingPage = false;
            });
        });
    }

    @Override
    public void open(Player player) {
        player.openInventory(getInventory());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (!event.getWhoClicked().equals(viewer))
            return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory))
            return;

        if (isBuildingPage)
            return; // Block clicks while loading

        int slot = event.getRawSlot();

        if (slot == 45 && page > 0) {
            page--;
            buildPage();
            return;
        }
        if (slot == 53 && (page + 1) * PAGE_SIZE < players.size()) {
            page++;
            buildPage();
            return;
        }
        if (slot == 49) {
            viewer.closeInventory();
            return;
        }

        int playerIndex = page * PAGE_SIZE + slot;
        if (playerIndex < 0 || playerIndex >= players.size())
            return;

        OfflinePlayer target = players.get(playerIndex);
        if (target.getName() == null)
            return; // Invalid

        viewer.closeInventory();
        copyPlayerIdentity(target);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(viewer)) {
            HandlerList.unregisterAll(this);
        }
    }

    private void copyPlayerIdentity(OfflinePlayer target) {
        LuckPermsHook lp = plugin.getHookManager().getLuckPermsHook();

        DisguiseProfile profile = new DisguiseProfile(viewer.getUniqueId());
        profile.setName(target.getName());
        profile.setSkin(target.getName());

        if (lp != null && lp.isEnabled()) {
            // Load all groups asynchronously
            lp.getAllGroupsAsync(target.getUniqueId()).thenAccept(groups -> {
                if (groups != null && !groups.isEmpty()) {
                    profile.setGroups(groups);

                    // For visuals, fetch the prefix of the primary group
                    lp.getPrimaryGroupAsync(target.getUniqueId()).thenAccept(primary -> {
                        if (primary != null && !primary.isEmpty()) {
                            profile.setPrimaryGroup(primary);
                            profile.setPrefix(lp.getPrefix(primary));
                            profile.setSuffix(lp.getSuffix(primary));
                        }
                        applyAndNotify(target, profile);
                    });
                } else {
                    applyAndNotify(target, profile);
                }
            });
        } else {
            applyAndNotify(target, profile);
        }
    }

    private void applyAndNotify(OfflinePlayer target, DisguiseProfile profile) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getDisguiseApplier().applyDisguise(viewer, profile);
            MessageManager msg = plugin.getMessageManager();
            viewer.sendMessage(msg.get("messages.copied-player", "name", target.getName()));
            notifyAdmins(target.getName());
        });
    }

    private void notifyAdmins(String targetName) {
        if (!plugin.getSettingsManager().notifyAdmins())
            return;
        String perm = plugin.getSettingsManager().getPermissionNotify();
        Component notify = plugin.getMessageManager().get(
                "messages.admin-notify", "real_name", viewer.getName(), "name", targetName);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(perm))
                p.sendMessage(notify);
        }
    }

    // ─── Item builders ────────────────────────────────────

    private ItemStack buildSkullSyncOrAsync(OfflinePlayer target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null && target.getName() != null) {
            meta.setOwningPlayer(target);
            meta.displayName(MessageManager.colorize("&#FFD700" + target.getName())
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager()
                    .get("gui-lore.uuid", "uuid", target.getUniqueId().toString().substring(0, 8) + "...")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            LuckPermsHook lp = plugin.getHookManager().getLuckPermsHook();
            if (lp != null && lp.isEnabled()) {
                // Fetch cached user or load
                String group = lp.getPrimaryGroupAsync(target.getUniqueId()).join();
                if (group != null && !group.isEmpty()) {
                    String prefix = lp.getPrefix(group);
                    if (!prefix.isEmpty()) {
                        lore.add(MessageManager
                                .colorize(
                                        plugin.getMessageManager().getRaw("gui-lore.rank", null, null) + prefix + group)
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    }
                }
            }
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().get("gui-lore.click-copy")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack buildSimpleItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageManager.colorize(name)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
