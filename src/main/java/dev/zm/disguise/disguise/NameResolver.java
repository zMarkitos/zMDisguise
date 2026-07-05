package dev.zm.disguise.disguise;

import dev.zm.disguise.models.DisguiseProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Centralized service for resolving player names within zMDisguise.
 *
 * Resolution priority:
 * 1. Real online player name (Bukkit.getPlayerExact)
 * 2. Disguise name (finds the player whose fake name matches)
 *
 * All code in the plugin should use this service instead of calling
 * Bukkit.getPlayer() directly, to ensure disguise-awareness.
 */
public class NameResolver {

    private final DisguiseService disguiseService;

    public NameResolver(DisguiseService disguiseService) {
        this.disguiseService = disguiseService;
    }

    // ─── Primary resolution ────────────────────────────────────────────────────

    /**
     * Resolves a name to an online Player using the full priority chain.
     * Returns null if not found.
     */
    public Player resolve(String name) {
        if (name == null || name.isEmpty())
            return null;

        // 1. Real name (if not disguised, or exact match)
        Player byReal = Bukkit.getPlayerExact(name);
        if (byReal != null)
            return byReal;

        // 2. Disguise name
        return findByFakeName(name).orElse(null);
    }

    // ─── Specific lookups ──────────────────────────────────────────────────────

    /**
     * Finds an online player whose current disguise name matches the given name.
     * Case-insensitive.
     */
    public Optional<Player> findByFakeName(String fakeName) {
        for (Map.Entry<UUID, DisguiseProfile> entry : disguiseService.getActiveDisguises().entrySet()) {
            DisguiseProfile profile = entry.getValue();
            if (profile.getName() != null && profile.getName().equalsIgnoreCase(fakeName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null)
                    return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    /**
     * Returns true if the desired disguise name is already in use by another
     * connected player (real name) or by another disguised player.
     *
     * The {@code target} player is excluded from the check so that
     * re-applying a disguise to the same person does not trigger a conflict.
     *
     * @param fakeName the desired disguise display name
     * @param target   the player who will receive the disguise
     */
    public boolean isNameTaken(String fakeName, Player target) {
        // Check real names of all online players (excluding target)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(target.getUniqueId()))
                continue;
            if (p.getName().equalsIgnoreCase(fakeName))
                return true;
        }

        // Check active disguise names (excluding target's own profile)
        for (Map.Entry<UUID, DisguiseProfile> entry : disguiseService.getActiveDisguises().entrySet()) {
            if (entry.getKey().equals(target.getUniqueId()))
                continue;
            DisguiseProfile profile = entry.getValue();
            if (profile.getName() != null && profile.getName().equalsIgnoreCase(fakeName))
                return true;
        }

        return false;
    }
}
