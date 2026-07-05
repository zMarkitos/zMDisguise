package dev.zm.disguise.tasks;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.config.SettingsManager;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarTask extends BukkitRunnable {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public BossBarTask(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    @Override
    public void run() {
        SettingsManager cfg = plugin.getSettingsManager();
        if (!cfg.isBossBarEnabled())
            return;

        // Show/update for disguised players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disguiseService.isDisguised(player)) {
                DisguiseProfile profile = disguiseService.getDisguise(player);
                updateBossBar(player, profile, cfg);
            } else {
                removeBossBar(player);
            }
        }

        // Remove for offline players
        activeBossBars.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private void updateBossBar(Player player, DisguiseProfile profile, SettingsManager cfg) {
        String rawTitle = MessageManager.applyPlaceholders(cfg.getBossBarTitle(), player, profile);
        Component title = MessageManager.colorize(rawTitle);

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(cfg.getBossBarColor().toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.YELLOW;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(cfg.getBossBarStyle().toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        float progress = (float) Math.max(0.0, Math.min(1.0, cfg.getBossBarProgress()));

        UUID uuid = player.getUniqueId();
        if (activeBossBars.containsKey(uuid)) {
            BossBar existing = activeBossBars.get(uuid);
            existing.name(title);
            existing.color(color);
            existing.overlay(overlay);
            existing.progress(progress);
        } else {
            BossBar bossBar = BossBar.bossBar(title, progress, color, overlay);
            player.showBossBar(bossBar);
            activeBossBars.put(uuid, bossBar);
        }
    }

    /** Call this method when the player removes their disguise. */
    public void removeBossBar(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }
}
