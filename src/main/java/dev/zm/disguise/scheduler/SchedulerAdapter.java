package dev.zm.disguise.scheduler;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public interface SchedulerAdapter {

    void runTask(JavaPlugin plugin, Runnable task);

    void runTaskAsync(JavaPlugin plugin, Runnable task);

    TaskHandle runTaskTimer(JavaPlugin plugin, Runnable task, long delay, long period);

    TaskHandle runTaskTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period);

    void runEntityTask(JavaPlugin plugin, Entity entity, Runnable task);

    interface TaskHandle {
        void cancel();
    }
}
