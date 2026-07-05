package dev.zm.disguise.hooks;

import dev.zm.disguise.zMDisguise;
import org.bukkit.Bukkit;

/**
 * Manages optional hooks (LuckPerms, PlaceholderAPI, SkinsRestorer).
 * Each hook is activated only if the corresponding plugin is installed.
 */
public class HookManager {

    private LuckPermsHook luckPermsHook;
    private PlaceholderHook placeholderHook;
    private SkinHook skinHook;

    public HookManager(zMDisguise plugin) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook();
            luckPermsHook.register();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }

        if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null) {
            skinHook = new SkinHook();
            skinHook.register();
        }
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public SkinHook getSkinHook() {
        return skinHook;
    }

    public boolean hasLuckPerms() {
        return luckPermsHook != null && luckPermsHook.isEnabled();
    }

    public boolean hasPlaceholderAPI() {
        return placeholderHook != null && placeholderHook.isEnabled();
    }

    public boolean hasSkinsRestorer() {
        return skinHook != null && skinHook.isEnabled();
    }
}
