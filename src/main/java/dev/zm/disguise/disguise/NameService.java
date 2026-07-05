package dev.zm.disguise.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.entity.Player;

public class NameService {

    public void applyName(Player player, String newName) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.setName(newName);
        player.setPlayerProfile(profile);
    }
}
