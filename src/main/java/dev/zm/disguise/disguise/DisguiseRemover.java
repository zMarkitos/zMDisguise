package dev.zm.disguise.disguise;

import dev.zm.disguise.refresh.RefreshManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Removes a disguise and fully restores the player's original identity:
 * - Restores the original PlayerProfile (name + skin).
 * - Resets the Adventure display name and player-list name.
 * - Triggers a visibility refresh for all nearby players.
 * - Cleans up LuckPerms transient groups.
 */
public class DisguiseRemover {

    private final DisguiseService disguiseService;
    private final RefreshManager refreshManager;

    public DisguiseRemover(DisguiseService disguiseService, RefreshManager refreshManager) {
        this.disguiseService = disguiseService;
        this.refreshManager = refreshManager;
    }

    public void removeDisguise(Player player) {
        if (!disguiseService.isDisguised(player))
            return;

        com.destroystokyo.paper.profile.PlayerProfile original = disguiseService.getOriginalProfile(player);

        if (original != null) {
            player.setPlayerProfile(original);
        }

        // Reset visual names back to the real player name
        player.displayName(Component.text(player.getName()));
        player.playerListName(Component.text(player.getName()));

        // Remove LuckPerms transient groups
        dev.zm.disguise.hooks.LuckPermsHook lp = dev.zm.disguise.zMDisguise.getInstance().getHookManager()
                .getLuckPermsHook();
        if (lp != null && lp.isEnabled()) {
            lp.removeTransientGroups(player);
        }

        // Cleans disguise from service
        disguiseService.removeDisguise(player);

        refreshManager.refreshPlayer(player);
    }
}
