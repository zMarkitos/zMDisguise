package dev.zm.disguise.refresh;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Forces a visibility refresh for a disguised player so that all nearby players
 * re-receive the player's updated profile (name + skin).
 *
 * Folia safety
 * On Folia, {@code hidePlayer} / {@code showPlayer} must be called on the
 * entity thread that owns the observer player's region. We schedule each
 * observer individually via
 * {@link dev.zm.disguise.scheduler.SchedulerAdapter#runEntityTask}
 * so the operation is always executed in the correct region context.
 *
 * On standard Paper the adapter falls back to a regular sync task, keeping
 * the original behaviour.
 */
public class RefreshManager {

    private final dev.zm.disguise.zMDisguise plugin;

    public RefreshManager(dev.zm.disguise.zMDisguise plugin) {
        this.plugin = plugin;
    }

    public void refreshPlayer(Player player) {
        dev.zm.disguise.scheduler.SchedulerAdapter scheduler = plugin.getScheduler();

        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (observer.equals(player))
                continue;

            // Capture for lambda
            final Player obs = observer;
            scheduler.runEntityTask(plugin, observer, () -> {
                if (obs.isOnline() && obs.canSee(player)) {
                    obs.hidePlayer(plugin, player);
                    obs.showPlayer(plugin, player);
                }
            });
        }
    }
}
