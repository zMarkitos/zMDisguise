package dev.zm.disguise.listeners;

import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * On quit the real identity (profile, names, LuckPerms groups) is always put back so nothing
 * disguised is ever cached or persisted by other plugins. With keep-disguise-on-relog the disguise
 * itself is stored and re-applied on the next join.
 */
public class PlayerQuitListener implements Listener {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;

    public PlayerQuitListener(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!disguiseService.isDisguised(player)) {
            // Not disguised: make sure no stale entry can re-disguise them on the next join.
            plugin.getStorageService().delete(player.getUniqueId());
            return;
        }
        if (plugin.getSettingsManager().keepDisguiseOnRelog()) {
            plugin.getStorageService().save(player.getUniqueId(), disguiseService.getDisguise(player));
        } else {
            plugin.getStorageService().delete(player.getUniqueId());
        }
        plugin.getDisguiseRemover().restoreIdentity(player, true);
        plugin.getVelocityMessenger().sendUndisguise(player);
    }
}
