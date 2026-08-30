package dev.zm.disguise.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulerAdapter implements SchedulerAdapter {

    @Override
    public void runTask(JavaPlugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runTaskAsync(JavaPlugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public TaskHandle runTaskTimer(JavaPlugin plugin, Runnable task, long delay, long period) {
        BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return bt::cancel;
    }

    @Override
    public TaskHandle runTaskTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) {
        BukkitTask bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        return bt::cancel;
    }

    @Override
    public void runEntityTask(JavaPlugin plugin, Entity entity, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
