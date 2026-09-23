package dev.zm.disguise.config;

import dev.zm.disguise.zMDisguise;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsManager {

    private final zMDisguise plugin;
    private FileConfiguration guisConfig;
    private java.io.File guisFile;

    private static final String PERMISSION_USE = "zmdisguise.use";
    private static final String PERMISSION_ADMIN = "zmdisguise.admin";
    private static final String PERMISSION_NOTIFY = "zmdisguise.notify";
    private static final String PERMISSION_RELOAD = "zmdisguise.reload";

    public SettingsManager(zMDisguise plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadGuis();
    }

    private void loadGuis() {
        guisFile = new java.io.File(plugin.getDataFolder(), "guis.yml");
        if (!guisFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }
        guisConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(guisFile);
    }

    public void reload() {
        plugin.reloadConfig();
        loadGuis();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    private FileConfiguration guis() {
        return guisConfig;
    }

    public String getLanguage() {
        return cfg().getString("language", "ES");
    }

    public boolean keepDisguiseOnRelog() {
        return cfg().getBoolean("keep-disguise-on-relog", false);
    }

    /** Keep the player's effective permissions while disguised (their real groups are detached). */
    public boolean keepPermissionsWhileDisguised() {
        return cfg().getBoolean("keep-permissions-while-disguised", true);
    }

    /** Replace the server's chat format with "prefix name: message" for disguised players. */
    public boolean chatOverrideFormat() {
        return cfg().getBoolean("chat.override-format", true);
    }

    public boolean notifyAdmins() {
        return cfg().getBoolean("notify-admins", true);
    }

    public String getPermissionUse() {
        return PERMISSION_USE;
    }

    public String getPermissionAdmin() {
        return PERMISSION_ADMIN;
    }

    public String getPermissionNotify() {
        return PERMISSION_NOTIFY;
    }

    public String getPermissionReload() {
        return PERMISSION_RELOAD;
    }

    /** Senders with this permission may still target disguised players in guarded commands. */
    public String getPermissionBypass() {
        return "zmdisguise.bypass";
    }

    /** Commands that must not resolve a disguise name as a target (tp & friends). */
    public java.util.List<String> hideFromCommands() {
        return cfg().getStringList("hide-from-commands");
    }

    public boolean isBossBarEnabled() {
        return cfg().getBoolean("bossbar.enabled", true);
    }

    public String getBossBarTitle() {
        return cfg().getString("bossbar.title", "&6Disguised as {name}");
    }

    public String getBossBarColor() {
        return cfg().getString("bossbar.color", "YELLOW");
    }

    public String getBossBarStyle() {
        return cfg().getString("bossbar.style", "SOLID");
    }

    public double getBossBarProgress() {
        return cfg().getDouble("bossbar.progress", 1.0);
    }

    public int getBossBarInterval() {
        return cfg().getInt("bossbar.update-interval", 20);
    }

    public boolean isActionBarEnabled() {
        return cfg().getBoolean("actionbar.enabled", true);
    }

    public String getActionBarMessage() {
        return cfg().getString("actionbar.message", "&6{prefix}{name}");
    }

    public int getActionBarInterval() {
        return cfg().getInt("actionbar.update-interval", 20);
    }

    public String getMenuTitle(String menuKey) {
        return guis().getString("menus." + menuKey + ".title", menuKey);
    }

    public int getMenuSize(String menuKey) {
        return guis().getInt("menus." + menuKey + ".size", 27);
    }

    public int getMenuSlot(String menuKey, String itemKey) {
        return guis().getInt("menus." + menuKey + ".slots." + itemKey + ".slot", 0);
    }

    public String getMenuItemMaterial(String menuKey, String itemKey) {
        return guis().getString("menus." + menuKey + ".slots." + itemKey + ".material", "STONE");
    }

    public String getMenuItemName(String menuKey, String itemKey) {
        return guis().getString("menus." + menuKey + ".slots." + itemKey + ".name", itemKey);
    }

    public java.util.List<String> getMenuItemLore(String menuKey, String itemKey) {
        return guis().getStringList("menus." + menuKey + ".slots." + itemKey + ".lore");
    }
}
