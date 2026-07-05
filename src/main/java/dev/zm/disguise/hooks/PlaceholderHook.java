package dev.zm.disguise.hooks;

import dev.zm.disguise.zMDisguise;
import dev.zm.disguise.models.DisguiseProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for zMDisguise.
 *
 * Available placeholders:
 * %zmdisguise_disguised% → "true" / "false"
 * %zmdisguise_name% → disguise name, or the real player name if not disguised
 * %zmdisguise_real_name% → always the real player name
 * %zmdisguise_prefix% → disguise prefix (empty if not disguised)
 * %zmdisguise_suffix% → disguise suffix (empty if not disguised)
 * %zmdisguise_group% → disguise group (empty if not disguised)
 * %zmdisguise_skin% → disguise skin name, or real name if not disguised
 */
public class PlaceholderHook extends PlaceholderExpansion {

    public boolean isEnabled() {
        return isRegistered();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zmdisguise";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zMDisguise";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline())
            return "";

        org.bukkit.entity.Player player = offlinePlayer.getPlayer();
        if (player == null)
            return "";

        DisguiseProfile profile = zMDisguise.getInstance()
                .getDisguiseService()
                .getDisguise(player);

        boolean disguised = profile != null;

        return switch (params.toLowerCase()) {
            case "disguised" -> disguised ? "true" : "false";

            // Returns the disguise name when disguised, real name otherwise
            case "name" -> disguised && profile.getName() != null
                    ? profile.getName()
                    : player.getName();

            case "real_name" -> player.getName();

            case "prefix" -> disguised && profile.getPrefix() != null
                    ? profile.getPrefix()
                    : "";

            case "suffix" -> disguised && profile.getSuffix() != null
                    ? profile.getSuffix()
                    : "";

            case "group" -> disguised && profile.getPrimaryGroup() != null
                    ? profile.getPrimaryGroup()
                    : "";

            case "skin" -> disguised && profile.getSkin() != null
                    ? profile.getSkin()
                    : player.getName();

            default -> null;
        };
    }
}
