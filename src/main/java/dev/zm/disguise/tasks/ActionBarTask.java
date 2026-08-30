package dev.zm.disguise.tasks;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.config.SettingsManager;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ActionBarTask {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;

    public ActionBarTask(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    /** Called by the scheduler on each tick interval. */
    public void run() {
        SettingsManager cfg = plugin.getSettingsManager();
        if (!cfg.isActionBarEnabled())
            return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!disguiseService.isDisguised(player))
                continue;

            DisguiseProfile profile = disguiseService.getDisguise(player);
            String raw = MessageManager.applyPlaceholders(cfg.getActionBarMessage(), player, profile);
            Component message = MessageManager.colorize(raw);
            player.sendActionBar(message);
        }
    }
}
