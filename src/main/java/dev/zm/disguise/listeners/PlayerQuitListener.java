package dev.zm.disguise.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.hooks.LuckPermsHook;
import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles disguise cleanup when a player disconnects.
 *
 * <h2>Bug fix for keep-disguise-on-relog</h2>
 * The previous implementation called {@code disguiseService.removeDisguise()}
 * directly, which only cleared the in-memory state but left the player's
 * {@link PlayerProfile} in the disguised state.  On the next login, Paper/Folia
 * can return this stale fake profile as the player's current profile, causing
 * {@link dev.zm.disguise.disguise.DisguiseService#addDisguise} to store the
 * <em>disguise</em> profile as the "original" — so un-disguising later would
 * restore the fake skin instead of the real one.
 *
 * <p>Fix: regardless of the relog setting, we always restore the real
 * {@link PlayerProfile} and clear LuckPerms transient groups before the player
 * leaves.  We skip the visual refresh since the player is already disconnecting.
 */
public class PlayerQuitListener implements Listener {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;

    public PlayerQuitListener(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!disguiseService.isDisguised(player))
            return;

        if (plugin.getSettingsManager().keepDisguiseOnRelog()) {
            // Persist the disguise profile so it can be re-applied on the next join
            plugin.getStorageService().save(player.getUniqueId(), disguiseService.getDisguise(player));
        } else {
            // Erase any stale saved profile so it never lingers across sessions
            plugin.getStorageService().delete(player.getUniqueId());
        }

        // ── Restore the real PlayerProfile ───────────────────────────────────
        // This is critical: without restoring the original profile here, Paper/Folia
        // may cache the disguised profile and hand it back on the next login,
        // causing DisguiseService to store the fake skin as the "original".
        PlayerProfile original = disguiseService.getOriginalProfile(player);
        if (original != null) {
            player.setPlayerProfile(original);
        }

        // ── Remove LuckPerms transient groups ────────────────────────────────
        LuckPermsHook lp = plugin.getHookManager().getLuckPermsHook();
        if (lp != null && lp.isEnabled()) {
            lp.removeTransientGroups(player);
        }

        // ── Notify proxy that real identity is restored ───────────────────────
        plugin.getVelocityMessenger().sendUndisguise(player);

        // ── Clear in-memory state ─────────────────────────────────────────────
        // No visual refresh needed — the player is disconnecting.
        disguiseService.removeDisguise(player);
    }
}