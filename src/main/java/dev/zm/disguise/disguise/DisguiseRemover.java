package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zm.disguise.hooks.LuckPermsHook;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.refresh.RefreshManager;
import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;

public class DisguiseRemover {

    private final DisguiseService disguiseService;
    private final RefreshManager refreshManager;

    public DisguiseRemover(DisguiseService disguiseService, RefreshManager refreshManager) {
        this.disguiseService = disguiseService;
        this.refreshManager = refreshManager;
    }

    /** Full undisguise: identity, groups, storage and a visual refresh. */
    public void removeDisguise(Player player) {
        if (!disguiseService.isDisguised(player))
            return;
        restoreIdentity(player, true);
        zMDisguise.getInstance().getStorageService().delete(player.getUniqueId());
        refreshManager.refreshPlayer(player);
        zMDisguise.getInstance().getVelocityMessenger().sendUndisguise(player);
    }

    /**
     * Puts the real profile, names and LuckPerms groups back without touching storage or
     * refreshing. Used by the quit handler and shutdown, where the disguise itself may be kept.
     */
    public void restoreIdentity(Player player, boolean clearService) {
        DisguiseProfile profile = disguiseService.getDisguise(player);
        PlayerProfile original = disguiseService.getOriginalProfile(player);
        if (original != null) {
            player.setPlayerProfile(original);
        }
        player.displayName(null);
        player.playerListName(null);
        zMDisguise.getInstance().getHookManager().getTabHook().setTabListName(player.getUniqueId(), null);

        LuckPermsHook lp = zMDisguise.getInstance().getHookManager().getLuckPermsHook();
        if (lp != null && lp.isEnabled()) {
            lp.restoreRealGroups(player.getUniqueId(), profile != null ? profile.getRealGroups() : null);
        }
        if (clearService) {
            disguiseService.removeDisguise(player);
        }
    }
}
