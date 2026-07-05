package dev.zm.disguise.config;

import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageManager {

    private final zMDisguise plugin;
    private FileConfiguration langConfig;

    private static final java.util.regex.Pattern HEX_PATTERN = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final java.util.regex.Pattern LEGACY_PATTERN = java.util.regex.Pattern
            .compile("[&§]([0-9a-fk-orA-FK-OR])");

    public MessageManager(zMDisguise plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String lang = plugin.getConfig().getString("language", "ES");
        String resourcePath = "langs/lang_" + lang + ".yml";
        File langFile = new File(plugin.getDataFolder(), resourcePath);

        // Save default lang file from JAR if missing
        if (!langFile.exists()) {
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                // Fallback to ES
                plugin.saveResource("langs/lang_ES.yml", false);
                langFile = new File(plugin.getDataFolder(), "langs/lang_ES.yml");
            }
        }

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults from JAR
        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            loaded.setDefaults(defaults);
        }

        this.langConfig = loaded;
    }

    public String getString(String path) {
        String message = langConfig.getString(path, "&#FF4444Message not found: " + path);
        String prefix = langConfig.getString("prefix", "");
        return message.replace("{prefix_plugin}", prefix);
    }

    public Component get(String path) {
        return colorize(getString(path));
    }

    public Component get(String path, Player player, DisguiseProfile profile) {
        String raw = getString(path);
        raw = applyPlaceholders(raw, player, profile);
        return colorize(raw);
    }

    public Component get(String path, String... replacements) {
        String raw = getString(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return colorize(raw);
    }

    public java.util.List<Component> getList(String path) {
        java.util.List<String> list = langConfig.getStringList(path);
        if (list == null || list.isEmpty())
            return java.util.Collections.singletonList(colorize("&#FF4444List not found: " + path));
        return list.stream().map(MessageManager::colorize).collect(java.util.stream.Collectors.toList());
    }

    public String getRaw(String path, Player player, DisguiseProfile profile) {
        return applyPlaceholders(getString(path), player, profile);
    }

    public static Component colorize(String raw) {
        if (raw == null)
            return Component.empty();

        java.util.regex.Matcher hexMatcher = HEX_PATTERN.matcher(raw);
        raw = hexMatcher.replaceAll("<#$1>");

        java.util.regex.Matcher legacyMatcher = LEGACY_PATTERN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (legacyMatcher.find()) {
            String code = legacyMatcher.group(1).toLowerCase();
            String tag = switch (code) {
                case "0" -> "<black>";
                case "1" -> "<dark_blue>";
                case "2" -> "<dark_green>";
                case "3" -> "<dark_aqua>";
                case "4" -> "<dark_red>";
                case "5" -> "<dark_purple>";
                case "6" -> "<gold>";
                case "7" -> "<gray>";
                case "8" -> "<dark_gray>";
                case "9" -> "<blue>";
                case "a" -> "<green>";
                case "b" -> "<aqua>";
                case "c" -> "<red>";
                case "d" -> "<light_purple>";
                case "e" -> "<yellow>";
                case "f" -> "<white>";
                case "k" -> "<obfuscated>";
                case "l" -> "<bold>";
                case "m" -> "<strikethrough>";
                case "n" -> "<underlined>";
                case "o" -> "<italic>";
                case "r" -> "<reset>";
                default -> "";
            };
            legacyMatcher.appendReplacement(sb, tag);
        }
        legacyMatcher.appendTail(sb);

        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(sb.toString());
    }

    /**
     * Applies internal placeholders to a raw string.
     */
    public static String applyPlaceholders(String raw, Player player, DisguiseProfile profile) {
        if (raw == null)
            return "";
        raw = raw.replace("{real_name}", player != null ? player.getName() : "");
        if (profile != null) {
            raw = raw.replace("{name}",
                    profile.getName() != null ? profile.getName() : (player != null ? player.getName() : ""));
            raw = raw.replace("{prefix}", profile.getPrefix() != null ? profile.getPrefix() : "");
            raw = raw.replace("{suffix}", profile.getSuffix() != null ? profile.getSuffix() : "");
            raw = raw.replace("{group}", profile.getPrimaryGroup() != null ? profile.getPrimaryGroup() : "");
            raw = raw.replace("{skin}",
                    profile.getSkin() != null ? profile.getSkin() : (player != null ? player.getName() : ""));
        }
        return raw;
    }
}
