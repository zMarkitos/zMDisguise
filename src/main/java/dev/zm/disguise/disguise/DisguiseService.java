package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zm.disguise.models.DisguiseProfile;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core in-memory registry for active disguises.
 *
 * Stores:
 * - Active disguise profiles (UUID → DisguiseProfile)
 * - Original Paper PlayerProfiles for restoration
 *
 * Also delegates alias registration to {@link AliasService}.
 */
public class DisguiseService {

    private final Map<UUID, DisguiseProfile> activeDisguises = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> originalProfiles = new ConcurrentHashMap<>();

    public DisguiseService() {
    }

    /**
     * Registers a disguise for the given player.
     * Stores the original profile on first disguise (not overwritten on
     * re-disguise).
     * Also registers the alias.
     */
    public void addDisguise(Player player, DisguiseProfile profile) {
        if (!originalProfiles.containsKey(player.getUniqueId())) {
            originalProfiles.put(player.getUniqueId(), player.getPlayerProfile());
        }
        activeDisguises.put(player.getUniqueId(), profile);
    }

    /**
     * Removes the disguise and clears the alias for the given player.
     */
    public void removeDisguise(Player player) {
        activeDisguises.remove(player.getUniqueId());
        originalProfiles.remove(player.getUniqueId());
    }

    public DisguiseProfile getDisguise(Player player) {
        return activeDisguises.get(player.getUniqueId());
    }

    public PlayerProfile getOriginalProfile(Player player) {
        return originalProfiles.get(player.getUniqueId());
    }

    public boolean isDisguised(Player player) {
        return activeDisguises.containsKey(player.getUniqueId());
    }

    /** Returns an unmodifiable view of all active disguises. */
    public Map<UUID, DisguiseProfile> getActiveDisguises() {
        return Collections.unmodifiableMap(activeDisguises);
    }
}
