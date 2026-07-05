package dev.zm.disguise.refresh;

import org.bukkit.entity.Player;

public class RefreshManager {

    private final dev.zm.disguise.zMDisguise plugin;

    public RefreshManager(dev.zm.disguise.zMDisguise plugin) {
        this.plugin = plugin;
    }

    public void refreshPlayer(Player player) {
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && online.canSee(player)) {
                online.hidePlayer(plugin, player);
                online.showPlayer(plugin, player);
            }
        }

    }
}
