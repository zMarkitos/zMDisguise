package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.refresh.RefreshManager;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DisguiseApplier {

    private final DisguiseService disguiseService;
    private final RefreshManager refreshManager;

    public DisguiseApplier(DisguiseService disguiseService, RefreshManager refreshManager) {
        this.disguiseService = disguiseService;
        this.refreshManager = refreshManager;
    }

    public void applyDisguise(Player player, DisguiseProfile profile) {
        // Register disguise in service
        disguiseService.addDisguise(player, profile);

        // Apply LuckPerms transient groups
        dev.zm.disguise.hooks.LuckPermsHook lp = zMDisguise.getInstance().getHookManager().getLuckPermsHook();
        if (lp != null && lp.isEnabled()) {
            lp.applyTransientGroups(player, profile.getGroups());
        }

        String skinName = profile.getSkin() != null ? profile.getSkin() : profile.getName();

        if (skinName != null && !skinName.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(zMDisguise.getInstance(), () -> {
                PlayerProfile skinProfile = Bukkit.createProfile(skinName);
                boolean hasSkin = false;
                try {
                    skinProfile.complete(); // blocking — fetches from Mojang
                    hasSkin = skinProfile.hasTextures();
                } catch (Exception ignored) {
                }

                final boolean finalHasSkin = hasSkin;
                Bukkit.getScheduler().runTask(zMDisguise.getInstance(), () -> {
                    applyProfileAndRefresh(player, profile, profile.getName(), skinProfile, finalHasSkin);
                });
            });
        } else {
            applyProfileAndRefresh(player, profile, profile.getName(), null, false);
        }
    }

    private void applyProfileAndRefresh(Player player, DisguiseProfile profile,
            String fakeName, PlayerProfile skinProfile, boolean hasSkin) {
        PlayerProfile paperProfile = player.getPlayerProfile();

        if (fakeName != null) {
            paperProfile.setName(fakeName);
        }

        // Apply skin textures
        if (hasSkin && skinProfile != null) {
            paperProfile.setTextures(skinProfile.getTextures());
        } else if (skinProfile != null) {
            paperProfile.getTextures().clear();
        }

        player.setPlayerProfile(paperProfile);

        // Set the visual display name to the clean disguise name (no alias suffix)
        if (profile.getName() != null) {
            String visual = (profile.getPrefix() != null ? profile.getPrefix() : "") + profile.getName();
            player.displayName(dev.zm.disguise.config.MessageManager.colorize(visual));
            player.playerListName(dev.zm.disguise.config.MessageManager.colorize(visual));
        }

        refreshManager.refreshPlayer(player);
    }

    /** Exposed for manual refresh calls. */
    public void refreshPlayer(Player player) {
        refreshManager.refreshPlayer(player);
    }
}
