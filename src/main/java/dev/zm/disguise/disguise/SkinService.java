package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;

public class SkinService {

    public void applySkin(Player player, String texture, String signature) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.setProperty(new ProfileProperty("textures", texture, signature));
        player.setPlayerProfile(profile);
    }
}
