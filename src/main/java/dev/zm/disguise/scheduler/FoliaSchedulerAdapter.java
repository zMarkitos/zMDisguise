package dev.zm.disguise.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerAdapter implements SchedulerAdapter {

    @Override
    public void runTask(JavaPlugin plugin, Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    @Override
    public void runTaskAsync(JavaPlugin plugin, Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public TaskHandle runTaskTimer(JavaPlugin plugin, Runnable task, long delay, long period) {
        ScheduledTask st = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduledTask -> task.run(), Math.max(1, delay), Math.max(1, period));
        return st::cancel;
    }

    @Override
    public TaskHandle runTaskTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) {
        long delayMs = delay * 50L;
        long periodMs = period * 50L;
        ScheduledTask st = plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(plugin, scheduledTask -> task.run(),
                        Math.max(1, delayMs), Math.max(50, periodMs), TimeUnit.MILLISECONDS);
        return st::cancel;
    }

    @Override
    public void runEntityTask(JavaPlugin plugin, Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }
}
