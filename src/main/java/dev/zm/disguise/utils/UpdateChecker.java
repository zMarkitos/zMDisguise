package dev.zm.disguise.utils;

import dev.zm.disguise.zMDisguise;
import org.bukkit.Bukkit;

import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {

    private final zMDisguise plugin;
    private final int resourceId;
    private boolean updateAvailable = false;
    private String latestVersion = "";
    private static final String RESOURCE_SLUG = "zmdisguise-advanced-player-disguise-system";

    public UpdateChecker(zMDisguise plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void fetch() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (InputStream inputStream = new URL(
                    "https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                    Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    this.latestVersion = scanner.next();
                    String currentVersion = plugin.getDescription().getVersion();
                    if (!currentVersion.equalsIgnoreCase(this.latestVersion)) {
                        this.updateAvailable = true;
                        plugin.getLogger()
                                .info("A new update for zMDisguise is available! Version: " + this.latestVersion);
                        plugin.getLogger()
                                .info("Download it at: https://www.spigotmc.org/resources/" + this.resourceId);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getResourceUrl() {
        return "https://www.spigotmc.org/resources/" + RESOURCE_SLUG + "." + resourceId + "/";
    }
}
