package dev.zm.disguise.gui;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.config.SettingsManager;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Plugin main menu. Opened with /disguise (no arguments).
 * Items and text are read directly from config.yml.
 *
 * The "Disguised Players" button is only visible to admins
 * (permission zmdisguise.admin).
 */
public class MainMenu implements Menu {

    private final zMDisguise plugin;

    public MainMenu(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return buildInventory(null);
    }

    @Override
    public void open(Player player) {
        player.openInventory(buildInventory(player));
    }

    private Inventory buildInventory(Player viewer) {
        SettingsManager cfg = plugin.getSettingsManager();

        String rawTitle = cfg.getMenuTitle("main-menu");
        Component title = MessageManager.colorize(rawTitle);
        int size = cfg.getMenuSize("main-menu");

        Inventory inv = org.bukkit.Bukkit.createInventory(null, size, title);

        // Border fill
        ItemStack glass = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) {
            gm.displayName(Component.empty());
            glass.setItemMeta(gm);
        }
        for (int i = 0; i < size; i++) {
            if (isEdge(i, size))
                inv.setItem(i, glass);
        }

        inv.setItem(cfg.getMenuSlot("main-menu", "copy-player"), buildFromConfig("main-menu", "copy-player"));
        inv.setItem(cfg.getMenuSlot("main-menu", "saved-disguises"), buildFromConfig("main-menu", "saved-disguises"));
        inv.setItem(cfg.getMenuSlot("main-menu", "remove-disguise"), buildFromConfig("main-menu", "remove-disguise"));

        // Admin-only: disguised-players button
        boolean isAdmin = viewer == null
                || viewer.hasPermission(plugin.getSettingsManager().getPermissionAdmin());
        if (isAdmin) {
            inv.setItem(cfg.getMenuSlot("main-menu", "disguised-players"),
                    buildFromConfig("main-menu", "disguised-players"));
        }

        return inv;
    }

    private ItemStack buildFromConfig(String menuKey, String itemKey) {
        SettingsManager cfg = plugin.getSettingsManager();
        String matData = cfg.getMenuItemMaterial(menuKey, itemKey);
        String name = cfg.getMenuItemName(menuKey, itemKey);
        List<String> lore = cfg.getMenuItemLore(menuKey, itemKey);

        ItemStack item = dev.zm.disguise.utils.HeadUtils.buildFromConfig(matData);
        return applyMeta(item, name, lore);
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        return applyMeta(new ItemStack(material), name, lore);
    }

    private ItemStack applyMeta(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageManager.colorize(name)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            List<Component> loreComponents = lore.stream()
                    .map(l -> MessageManager.colorize(l)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isEdge(int slot, int size) {
        int row = slot / 9;
        int col = slot % 9;
        int rows = size / 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }
}
