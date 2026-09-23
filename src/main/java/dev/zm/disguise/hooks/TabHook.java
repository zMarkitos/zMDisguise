package dev.zm.disguise.hooks;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional hook for TAB (NEZNAMY).
 *
 * <p>TAB owns the tablist entries it manages, so a {@code playerListName} set by this plugin is
 * overwritten on TAB's next refresh and the player list keeps showing the real name. Pushing the
 * name through TAB's own API fixes that.
 *
 * <p>Reflection keeps TAB a true soft dependency: no compile-time artifact, and every failure
 * (TAB missing, reloading, API change) falls back to the vanilla name silently.
 */
public class TabHook {

    private boolean enabled;
    private Method getInstance;
    private Method getPlayer;
    private Method getFormatManager;
    private Method setName;

    public TabHook() {
        try {
            if (Bukkit.getPluginManager().getPlugin("TAB") == null)
                return;
            Class<?> api = Class.forName("me.neznamy.tab.api.TabAPI");
            Class<?> tabPlayer = Class.forName("me.neznamy.tab.api.TabPlayer");
            Class<?> formatManager = Class.forName("me.neznamy.tab.api.tablist.TabListFormatManager");
            getInstance = api.getMethod("getInstance");
            getPlayer = api.getMethod("getPlayer", UUID.class);
            getFormatManager = api.getMethod("getTabListFormatManager");
            setName = formatManager.getMethod("setName", tabPlayer, String.class);
            enabled = getInstance.invoke(null) != null;
        } catch (Throwable ignored) {
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Sets the tablist name TAB shows for the player, or restores the real one when {@code name} is null. */
    public void setTabListName(UUID uuid, String name) {
        if (!enabled)
            return;
        try {
            Object api = getInstance.invoke(null);
            if (api == null)
                return;
            Object player = getPlayer.invoke(api, uuid);
            Object manager = getFormatManager.invoke(api);
            if (player == null || manager == null)
                return;
            setName.invoke(manager, player, name);
        } catch (Throwable ignored) {
            // TAB reloading or a newer API: the vanilla playerListName still applies
        }
    }
}
