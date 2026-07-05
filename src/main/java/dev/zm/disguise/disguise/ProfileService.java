package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;

public class ProfileService {

    public void updatePlayerProfile(Player player, String name, String texture, String signature) {
        PlayerProfile profile = player.getPlayerProfile();
        if (name != null && !name.isEmpty()) {
            profile.setName(name);
        }
        if (texture != null && !texture.isEmpty()) {
            profile.setProperty(new ProfileProperty("textures", texture, signature));
        }
        // PlayerProfile API from Paper safely updates the visual data
        // across the server without requiring manual packet injection for self-updates
        player.setPlayerProfile(profile);
    }
}
