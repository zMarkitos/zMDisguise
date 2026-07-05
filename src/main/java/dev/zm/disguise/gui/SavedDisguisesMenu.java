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
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Menu that shows the user's saved disguises.
 */
public class SavedDisguisesMenu implements Menu, Listener {

    private final zMDisguise plugin;
    private final Player viewer;
    private Inventory inventory;
    private List<String> savedNames = new ArrayList<>();
    private Map<String, DisguiseProfile> savedProfiles;
    private boolean isBuilding = false;

    public SavedDisguisesMenu(zMDisguise plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void openAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savedProfiles = plugin.getStorageService().getSavedProfiles(viewer.getUniqueId());
            savedNames = new ArrayList<>(savedProfiles.keySet());

            Bukkit.getScheduler().runTask(plugin, () -> {
                open(viewer);
            });
        });
    }

    @Override
    public Inventory getInventory() {
        dev.zm.disguise.config.SettingsManager cfg = plugin.getSettingsManager();
        Component title = MessageManager.colorize(cfg.getMenuTitle("saved-disguises-selector"));
        int size = cfg.getMenuSize("saved-disguises-selector");
        inventory = Bukkit.createInventory(null, size, title);
        buildPage();
        return inventory;
    }

    private void buildPage() {
        if (isBuilding)
            return;
        isBuilding = true;
        inventory.clear();

        int size = inventory.getSize();
        for (int i = 0; i < Math.min(savedNames.size(), size - 9); i++) {
            String saveName = savedNames.get(i);
            DisguiseProfile profile = savedProfiles.get(saveName);

            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MessageManager.colorize("&#FFD700" + saveName)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(plugin.getMessageManager().get("gui-lore.name", "name", profile.getName())
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                if (profile.getPrimaryGroup() != null && !profile.getPrimaryGroup().isEmpty()) {
                    lore.add(MessageManager
                            .colorize(plugin.getMessageManager().getRaw("gui-lore.rank", null, null) + "&#FFFFFF"
                                    + profile.getPrimaryGroup())
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                lore.add(plugin.getMessageManager().get("gui-lore.click-equip")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                lore.add(plugin.getMessageManager().get("gui-lore.click-delete")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MessageManager.colorize("&#FF4444✗ &#FFFFFFVolver")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(size - 5, back);

        isBuilding = false;
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

        if (isBuilding)
            return;

        int slot = event.getRawSlot();
        int size = inventory.getSize();

        if (slot == size - 5) {
            viewer.closeInventory();
            new MainMenu(plugin).open(viewer);
            return;
        }

        if (slot < 0 || slot >= Math.min(savedNames.size(), size - 9))
            return;

        String saveName = savedNames.get(slot);

        if (event.getClick() == ClickType.DROP) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getStorageService().deleteSavedProfile(viewer.getUniqueId(), saveName);
                savedNames.remove(saveName);
                savedProfiles.remove(saveName);
                Bukkit.getScheduler().runTask(plugin, this::buildPage);
            });
            return;
        }

        DisguiseProfile profile = savedProfiles.get(saveName);
        if (profile == null)
            return;

        viewer.closeInventory();
        plugin.getDisguiseApplier().applyDisguise(viewer, profile);
        viewer.sendMessage(
                plugin.getMessageManager().get("messages.disguised-success", "name", profile.getName(), "prefix", ""));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(viewer)) {
            HandlerList.unregisterAll(this);
        }
    }
}
