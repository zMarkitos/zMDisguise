package dev.zm.disguise.listeners;

import dev.zm.disguise.disguise.DisguiseApplier;
import dev.zm.disguise.disguise.DisguiseRemover;
import dev.zm.disguise.gui.DisguisedPlayersMenu;
import dev.zm.disguise.gui.MainMenu;
import dev.zm.disguise.gui.PlayerSelectorMenu;
import dev.zm.disguise.gui.SavedDisguisesMenu;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all clicks within the MainMenu.
 * Registered persistently for the plugin's lifetime.
 */
public class MenuClickListener implements Listener {

    private final zMDisguise plugin;
    private final DisguiseRemover disguiseRemover;

    public MenuClickListener(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseRemover = plugin.getDisguiseRemover();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Identify menu by its rendered title (plain text comparison)
        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        String rawMainTitle = PlainTextComponentSerializer.plainText()
                .serialize(dev.zm.disguise.config.MessageManager.colorize(
                        plugin.getSettingsManager().getMenuTitle("main-menu")));

        if (!title.equals(rawMainTitle)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null
                || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot        = event.getRawSlot();
        int slotCopy    = plugin.getSettingsManager().getMenuSlot("main-menu", "copy-player");
        int slotSaved   = plugin.getSettingsManager().getMenuSlot("main-menu", "saved-disguises");
        int slotRemove  = plugin.getSettingsManager().getMenuSlot("main-menu", "remove-disguise");
        int slotAdmin   = plugin.getSettingsManager().getMenuSlot("main-menu", "disguised-players");

        if (slot == slotCopy) {
            player.closeInventory();
            new PlayerSelectorMenu(plugin, player).openAsync();

        } else if (slot == slotSaved) {
            player.closeInventory();
            new SavedDisguisesMenu(plugin, player).openAsync();

        } else if (slot == slotRemove) {
            player.closeInventory();
            disguiseRemover.removeDisguise(player);
            player.sendMessage(plugin.getMessageManager().get("messages.undisguised-success"));

        } else if (slot == slotAdmin) {
            if (!player.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                player.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return;
            }
            player.closeInventory();
            new DisguisedPlayersMenu(plugin, player).openAsync();
        }
    }
}
