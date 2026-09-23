package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zm.disguise.hooks.LuckPermsHook;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.refresh.RefreshManager;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Applies a disguise to a player: swaps LuckPerms groups, name and skin, then refreshes the
 * player for everyone. Skin fetching is asynchronous; player state is touched on the entity thread.
 */
public class DisguiseApplier {

    private final DisguiseService disguiseService;
    private final RefreshManager refreshManager;

    public DisguiseApplier(DisguiseService disguiseService, RefreshManager refreshManager) {
        this.disguiseService = disguiseService;
        this.refreshManager = refreshManager;
    }

    public void applyDisguise(Player player, DisguiseProfile profile) {
        zMDisguise plugin = zMDisguise.getInstance();
        disguiseService.addDisguise(player, profile);

        LuckPermsHook lp = plugin.getHookManager().getLuckPermsHook();
        if (lp != null && lp.isEnabled()) {
            profile.setRealGroups(lp.applyDisguiseGroups(player, profile.getGroups(), profile.getRealGroups(),
                    plugin.getSettingsManager().keepPermissionsWhileDisguised()));
        }
        // Persist right away so a crash or kill can never leave the player without their real groups.
        plugin.getStorageService().save(player.getUniqueId(), profile);

        String skinName = profile.getSkin() != null ? profile.getSkin() : profile.getName();
        if (skinName == null || skinName.isEmpty()) {
            applyProfileAndRefresh(player, profile, null, false);
            return;
        }

        // Someone with that skin online (or cached with textures)? Copy their profile directly:
        // no Mojang round-trip, and it also covers Bedrock/Floodgate skins Mojang does not know.
        PlayerProfile local = localProfileWithTextures(skinName, player);
        if (local != null) {
            applyProfileAndRefresh(player, profile, local, true);
            return;
        }

        plugin.getScheduler().runTaskAsync(plugin, () -> {
            // By uuid when we know the Java account (name changes do not matter then), else by name.
            java.util.UUID byUuid = javaUuid(profile.getSkinUuid());
            PlayerProfile skinProfile = byUuid != null ? Bukkit.createProfile(byUuid, skinName) : Bukkit.createProfile(skinName);
            boolean hasSkin = false;
            try {
                skinProfile.complete(); // blocking Mojang lookup
                hasSkin = skinProfile.hasTextures();
            } catch (Exception ignored) {
            }
            if (!hasSkin && byUuid != null) {
                try {
                    skinProfile = Bukkit.createProfile(skinName);
                    skinProfile.complete();
                    hasSkin = skinProfile.hasTextures();
                } catch (Exception ignored) {
                }
            }
            final boolean finalHasSkin = hasSkin;
            final PlayerProfile finalProfile = skinProfile;
            plugin.getScheduler().runEntityTask(plugin, player, () -> {
                if (!player.isOnline() || disguiseService.getDisguise(player) != profile)
                    return; // left, or disguised as something else while we were fetching
                applyProfileAndRefresh(player, profile, finalProfile, finalHasSkin);
            });
        });
    }

    /** The uuid if it is a real Java (v4) account; Bedrock/Floodgate and offline-mode ids have nothing on Mojang. */
    private static java.util.UUID javaUuid(String raw) {
        if (raw == null || raw.isEmpty())
            return null;
        try {
            java.util.UUID u = java.util.UUID.fromString(raw);
            return u.version() == 4 && u.getMostSignificantBits() != 0L ? u : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PlayerProfile localProfileWithTextures(String name, Player self) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && !online.equals(self)) {
            PlayerProfile p = online.getPlayerProfile();
            if (p.hasTextures())
                return p;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && !cached.getUniqueId().equals(self.getUniqueId())) {
            PlayerProfile p = cached.getPlayerProfile();
            if (p.hasTextures())
                return p;
        }
        return null;
    }

    private void applyProfileAndRefresh(Player player, DisguiseProfile profile, PlayerProfile skinProfile,
            boolean hasSkin) {
        PlayerProfile paperProfile = player.getPlayerProfile();
        if (profile.getName() != null) {
            paperProfile.setName(profile.getName());
        }
        if (hasSkin && skinProfile != null) {
            paperProfile.setTextures(skinProfile.getTextures());
        } else {
            paperProfile.getTextures().clear(); // default Steve/Alex: showing the real skin would give the player away
        }
        player.setPlayerProfile(paperProfile);

        // Plain fake name only: the rank prefix now comes from LuckPerms like for any other player,
        // so putting it in the display name would show it twice in chat and tab.
        if (profile.getName() != null) {
            player.displayName(Component.text(profile.getName()));
            player.playerListName(Component.text(profile.getName()));
            zMDisguise.getInstance().getHookManager().getTabHook().setTabListName(player.getUniqueId(), profile.getName());
        }

        refreshManager.refreshPlayer(player);
        zMDisguise.getInstance().getVelocityMessenger().sendDisguise(player, profile);
    }

    /** Exposed for manual refresh calls. */
    public void refreshPlayer(Player player) {
        refreshManager.refreshPlayer(player);
    }
}
