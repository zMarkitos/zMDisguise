package dev.zm.disguise.storage;

import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YamlStorage implements StorageService {

    private final zMDisguise plugin;
    private File storageFile;
    private FileConfiguration config;

    public YamlStorage(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        storageFile = new File(plugin.getDataFolder(), "disguises.yml");
        if (!storageFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create disguises.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(storageFile);
    }

    @Override
    public void save(UUID playerUuid, DisguiseProfile profile) {
        String path = playerUuid.toString();
        config.set(path + ".name", profile.getName());
        config.set(path + ".skin", profile.getSkin());
        config.set(path + ".prefix", profile.getPrefix());
        config.set(path + ".suffix", profile.getSuffix());
        config.set(path + ".primaryGroup", profile.getPrimaryGroup());
        config.set(path + ".groups", profile.getGroups());
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save disguise: " + e.getMessage());
        }
    }

    @Override
    public DisguiseProfile load(UUID playerUuid) {
        String path = playerUuid.toString();
        if (!config.contains(path))
            return null;
        DisguiseProfile profile = new DisguiseProfile(playerUuid);
        profile.setName(config.getString(path + ".name"));
        profile.setSkin(config.getString(path + ".skin"));
        profile.setPrefix(config.getString(path + ".prefix"));
        profile.setSuffix(config.getString(path + ".suffix"));
        profile.setPrimaryGroup(config.getString(path + ".primaryGroup"));
        if (config.contains(path + ".groups")) {
            profile.setGroups(config.getStringList(path + ".groups"));
        } else if (config.contains(path + ".group")) {
            String g = config.getString(path + ".group");
            profile.setPrimaryGroup(g);
            profile.addGroup(g);
        }
        return profile;
    }

    @Override
    public void delete(UUID playerUuid) {
        config.set(playerUuid.toString(), null);
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete disguise: " + e.getMessage());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void saveSavedProfile(UUID playerUuid, String saveName, DisguiseProfile profile) {
        String path = "saved." + playerUuid.toString() + "." + saveName.toLowerCase();
        config.set(path + ".name", profile.getName());
        config.set(path + ".skin", profile.getSkin());
        config.set(path + ".prefix", profile.getPrefix());
        config.set(path + ".suffix", profile.getSuffix());
        config.set(path + ".primaryGroup", profile.getPrimaryGroup());
        config.set(path + ".groups", profile.getGroups());
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save saved profile: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, DisguiseProfile> getSavedProfiles(UUID playerUuid) {
        java.util.Map<String, DisguiseProfile> map = new java.util.HashMap<>();
        String path = "saved." + playerUuid.toString();
        if (!config.contains(path))
            return map;
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null)
            return map;

        for (String saveName : section.getKeys(false)) {
            DisguiseProfile profile = new DisguiseProfile(playerUuid);
            String prefix = path + "." + saveName;
            profile.setName(config.getString(prefix + ".name"));
            profile.setSkin(config.getString(prefix + ".skin"));
            profile.setPrefix(config.getString(prefix + ".prefix"));
            profile.setSuffix(config.getString(prefix + ".suffix"));
            profile.setPrimaryGroup(config.getString(prefix + ".primaryGroup"));
            if (config.contains(prefix + ".groups")) {
                profile.setGroups(config.getStringList(prefix + ".groups"));
            } else if (config.contains(prefix + ".group")) {
                String g = config.getString(prefix + ".group");
                profile.setPrimaryGroup(g);
                profile.addGroup(g);
            }
            map.put(saveName, profile);
        }
        return map;
    }

    @Override
    public void deleteSavedProfile(UUID playerUuid, String saveName) {
        String path = "saved." + playerUuid.toString() + "." + saveName.toLowerCase();
        config.set(path, null);
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete saved profile: " + e.getMessage());
        }
    }
}
