package dev.zm.disguise.gui;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * GUI that shows all currently disguised players.
 * Requires zmdisguise.admin permission.
 *
 * Click on a head → teleport the admin to that player.
 * The menu is safe against disconnects; missing players are skipped silently.
 */
public class DisguisedPlayersMenu implements Menu, Listener {

    private final zMDisguise plugin;
    private final Player viewer;
    private Inventory inventory;

    /** Snapshot of disguised UUIDs at menu open time (ordered). */
    private final List<UUID> disguisedUuids = new ArrayList<>();

    public DisguisedPlayersMenu(zMDisguise plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void openAsync() {
        // Snapshot on the main thread, build heads async
        for (UUID uuid : plugin.getDisguiseService().getActiveDisguises().keySet()) {
            disguisedUuids.add(uuid);
        }

        if (disguisedUuids.isEmpty()) {
            viewer.sendMessage(plugin.getMessageManager().get("messages.no-disguised-players"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Build skull items off-thread (profile lookup can be slow)
            List<ItemStack> skulls = buildSkulls();
            Bukkit.getScheduler().runTask(plugin, () -> {
                open(viewer);
                for (int i = 0; i < Math.min(skulls.size(), inventory.getSize() - 9); i++) {
                    inventory.setItem(i, skulls.get(i));
                }
            });
        });
    }

    @Override
    public Inventory getInventory() {
        dev.zm.disguise.config.SettingsManager cfg = plugin.getSettingsManager();
        Component title = MessageManager.colorize(cfg.getMenuTitle("disguised-players-menu"));
        int size = cfg.getMenuSize("disguised-players-menu");
        inventory = Bukkit.createInventory(null, size, title);

        // Fill loading placeholders
        ItemStack loading = simple(Material.CLOCK, plugin.getMessageManager().getRaw("gui-lore.loading", null, null));
        for (int i = 0; i < Math.min(disguisedUuids.size(), size - 9); i++) {
            inventory.setItem(i, loading);
        }

        // Bottom bar
        ItemStack filler = simple(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Component.empty());
            filler.setItemMeta(fm);
        }
        for (int i = size - 9; i < size; i++)
            inventory.setItem(i, filler);
        inventory.setItem(size - 5, simple(Material.BARRIER, plugin.getMessageManager().getString("gui-items.back")));

        return inventory;
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
        if (!(event.getWhoClicked() instanceof Player clicker))
            return;
        if (!clicker.equals(viewer))
            return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory))
            return;

        int slot = event.getRawSlot();
        int size = inventory.getSize();

        if (slot == size - 5) {
            viewer.closeInventory();
            new MainMenu(plugin).open(viewer);
            return;
        }

        if (slot < 0 || slot >= Math.min(disguisedUuids.size(), size - 9))
            return;

        UUID targetUuid = disguisedUuids.get(slot);
        Player target = Bukkit.getPlayer(targetUuid);

        viewer.closeInventory();

        if (target == null || !target.isOnline()) {
            viewer.sendMessage(plugin.getMessageManager().get("messages.player-disconnected"));
            return;
        }

        if (!viewer.hasPermission("minecraft.command.teleport") &&
                !viewer.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
            viewer.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
            return;
        }

        viewer.teleport(target.getLocation());
        viewer.sendMessage(plugin.getMessageManager().get("messages.teleported-to", "name", target.getName()));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(viewer)) {
            HandlerList.unregisterAll(this);
        }
    }

    private List<ItemStack> buildSkulls() {
        List<ItemStack> result = new ArrayList<>();
        for (UUID uuid : disguisedUuids) {
            Player p = Bukkit.getPlayer(uuid);
            DisguiseProfile profile = plugin.getDisguiseService().getActiveDisguises().get(uuid);
            result.add(buildSkull(p, profile));
        }
        return result;
    }

    private ItemStack buildSkull(Player target, DisguiseProfile profile) {
        if (target == null || profile == null) {
            return simple(Material.SKELETON_SKULL, "&#888888Jugador desconectado");
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null)
            return skull;

        meta.setOwningPlayer(target);
        meta.displayName(MessageManager.colorize("&#FFD700" + target.getName())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().get("gui-lore.real-name", "name", target.getName())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(plugin.getMessageManager().get("gui-lore.disguise", "name", profile.getName())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        if (profile.getPrimaryGroup() != null && !profile.getPrimaryGroup().isEmpty()) {
            lore.add(plugin.getMessageManager().get("gui-lore.group", "group", profile.getPrimaryGroup())
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            if (profile.getGroups() != null && profile.getGroups().size() > 1) {
                lore.add(plugin.getMessageManager()
                        .get("gui-lore.extra-ranks", "amount", String.valueOf(profile.getGroups().size() - 1))
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
        }

        // Time disguised
        long elapsed = System.currentTimeMillis() - profile.getDisguisedAt();
        String timeStr = formatElapsed(elapsed);
        lore.add(plugin.getMessageManager().get("gui-lore.time", "time", timeStr)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        // Location
        lore.add(plugin.getMessageManager().get("gui-lore.world", "world", target.getWorld().getName())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(plugin.getMessageManager().get("gui-lore.pos",
                "x", String.format(java.util.Locale.US, "%.0f", target.getLocation().getX()),
                "y", String.format(java.util.Locale.US, "%.0f", target.getLocation().getY()),
                "z", String.format(java.util.Locale.US, "%.0f", target.getLocation().getZ()))
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(plugin.getMessageManager().get("gui-lore.click-teleport")
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private String formatElapsed(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        if (minutes > 0)
            return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private ItemStack simple(Material material, String name) {
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
