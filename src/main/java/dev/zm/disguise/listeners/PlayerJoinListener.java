package dev.zm.disguise.listeners;

import dev.zm.disguise.zMDisguise;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * On player join: restore disguise if keep-disguise-on-relog is enabled.
 */
public class PlayerJoinListener implements Listener {

    private final zMDisguise plugin;

    public PlayerJoinListener(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        if (plugin.getSettingsManager().keepDisguiseOnRelog()) {
            var profile = plugin.getStorageService().load(player.getUniqueId());
            if (profile != null) {
                plugin.getDisguiseApplier().applyDisguise(player, profile);
            }
        }

        if (player.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
            dev.zm.disguise.utils.UpdateChecker checker = plugin.getUpdateChecker();
            if (checker != null && checker.isUpdateAvailable()) {
                // Send chat message
                player.sendMessage(plugin.getMessageManager().get("update-notify", "version",
                        checker.getLatestVersion(), "link", checker.getResourceUrl()));

                // Send Title
                net.kyori.adventure.text.Component title = plugin.getMessageManager().get("update-title.title");
                net.kyori.adventure.text.Component subtitle = plugin.getMessageManager().get("update-title.subtitle",
                        "old_version", plugin.getDescription().getVersion(), "new_version", checker.getLatestVersion());
                player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle,
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(500),
                                java.time.Duration.ofMillis(4000),
                                java.time.Duration.ofMillis(1000))));
            }
        }
    }
}
