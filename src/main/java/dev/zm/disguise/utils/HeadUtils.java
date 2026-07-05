package dev.zm.disguise.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadUtils {

    private static final String HEAD_PREFIX = "basehead-";
    private static final Map<String, ItemStack> HEAD_CACHE = new ConcurrentHashMap<>();

    public static ItemStack buildFromConfig(String materialData) {
        if (materialData == null || materialData.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        if (materialData.toLowerCase().startsWith(HEAD_PREFIX)) {
            String base64 = materialData.substring(HEAD_PREFIX.length());
            return buildTexturedHead(base64).clone();
        }

        try {
            return new ItemStack(Material.valueOf(materialData.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE);
        }
    }

    private static ItemStack buildTexturedHead(String base64Texture) {
        ItemStack cached = HEAD_CACHE.get(base64Texture);
        if (cached != null) {
            return cached;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", base64Texture));
                meta.setPlayerProfile(profile);
                skull.setItemMeta(meta);
            } catch (Exception ignored) {
            }
        }

        HEAD_CACHE.put(base64Texture, skull);
        return skull;
    }
}