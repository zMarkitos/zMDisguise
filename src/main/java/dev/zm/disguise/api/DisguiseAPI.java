package dev.zm.disguise.api;

import dev.zm.disguise.disguise.DisguiseApplier;
import dev.zm.disguise.disguise.DisguiseRemover;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;

/**
 * Public API for zMDisguise.
 * Other plugins can access disguise state using this class.
 *
 * DisguiseAPIImpl.isDisguised(player);
 * DisguiseAPIImpl.getDisguise(player);
 * DisguiseAPIImpl.applyDisguise(player, profile);
 * DisguiseAPIImpl.removeDisguise(player);
 */
public class DisguiseAPI {

    private static zMDisguise plugin;

    /** Called internally by zMDisguise on enable. */
    public static void init(zMDisguise pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * @return true if the player is currently disguised.
     */
    public static boolean isDisguised(Player player) {
        return plugin.getDisguiseService().isDisguised(player);
    }

    /**
     * @return the active DisguiseProfile for the player, or null if not disguised.
     */
    public static DisguiseProfile getDisguise(Player player) {
        return plugin.getDisguiseService().getDisguise(player);
    }

    /**
     * @return the disguised name, or the real name if not disguised.
     */
    public static String getDisguiseName(Player player) {
        DisguiseProfile profile = getDisguise(player);
        return profile != null && profile.getName() != null ? profile.getName() : player.getName();
    }

    /**
     * Applies a DisguiseProfile to a player.
     */
    public static void applyDisguise(Player player, DisguiseProfile profile) {
        plugin.getDisguiseApplier().applyDisguise(player, profile);
    }

    /**
     * Removes the disguise from a player and restores their original identity.
     */
    public static void removeDisguise(Player player) {
        plugin.getDisguiseRemover().removeDisguise(player);
    }

    /**
     * Forces a visual refresh for a player (re-syncs tablist and nearby players).
     */
    public static void refresh(Player player) {
        plugin.getDisguiseApplier().refreshPlayer(player);
    }
}
