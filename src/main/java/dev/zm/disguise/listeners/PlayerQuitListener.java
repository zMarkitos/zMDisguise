package dev.zm.disguise.listeners;

import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.zMDisguise;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears disguise data when a player disconnects.
 * The disguise is only persisted to storage when keep-disguise-on-relog
 * is enabled; otherwise the saved profile (if any) is removed as well,
 * so a stale disguise from a previous session never lingers.
 */
public class PlayerQuitListener implements Listener {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;

    public PlayerQuitListener(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (!disguiseService.isDisguised(player))
            return;

        if (plugin.getSettingsManager().keepDisguiseOnRelog()) {
            plugin.getStorageService().save(player.getUniqueId(), disguiseService.getDisguise(player));
        } else {
            plugin.getStorageService().delete(player.getUniqueId());
        }

        disguiseService.removeDisguise(player);
    }
}