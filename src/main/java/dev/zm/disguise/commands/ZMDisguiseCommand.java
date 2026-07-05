package dev.zm.disguise.commands;

import dev.zm.disguise.zMDisguise;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /zmdisguise command — Administration subcommands.
 * /zmdisguise reload
 */
public class ZMDisguiseCommand implements CommandExecutor, TabCompleter {

    private final zMDisguise plugin;

    public ZMDisguiseCommand(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            for (net.kyori.adventure.text.Component line : plugin.getMessageManager().getList("messages.help")) {
                sender.sendMessage(line);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            String perm = plugin.getSettingsManager().getPermissionReload();
            if (!sender.hasPermission(perm)) {
                sender.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }

            // Reload config & language
            plugin.getSettingsManager().reload();
            plugin.getMessageManager().load();

            sender.sendMessage(plugin.getMessageManager().get("messages.reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("whois")) {
            if (!sender.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                sender.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(
                        plugin.getMessageManager().colorize("&#FF4444Uso: /zmdisguise whois <nombre_falso>"));
                return true;
            }
            String fakeName = args[1];
            org.bukkit.entity.Player found = null;
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (plugin.getDisguiseService().isDisguised(p)) {
                    dev.zm.disguise.models.DisguiseProfile profile = plugin.getDisguiseService().getDisguise(p);
                    if (profile.getName() != null && profile.getName().equalsIgnoreCase(fakeName)) {
                        found = p;
                        break;
                    }
                }
            }
            if (found != null) {
                sender.sendMessage(plugin.getMessageManager().colorize(
                        "&#00FF7FPlayer &#FFD700" + fakeName + " &#00FF7Fis actually: &#FFFFFF" + found.getName()));
            } else {
                sender.sendMessage(plugin.getMessageManager()
                        .colorize("&#FF4444No disguised player found with the name: " + fakeName));
            }
            return true;
        }

        // Unknown subcommand — show help
        for (net.kyori.adventure.text.Component line : plugin.getMessageManager().getList("messages.help")) {
            sender.sendMessage(line);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help");
            if (sender.hasPermission(plugin.getSettingsManager().getPermissionReload())) {
                options.add("reload");
            }
            if (sender.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                options.add("whois");
            }
            String partial = args[0].toLowerCase();
            return options.stream().filter(o -> o.startsWith(partial)).collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("whois")) {
            if (sender.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                String partial = args[1].toLowerCase();
                List<String> fakeNames = new ArrayList<>();
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (plugin.getDisguiseService().isDisguised(p)) {
                        dev.zm.disguise.models.DisguiseProfile profile = plugin.getDisguiseService().getDisguise(p);
                        if (profile.getName() != null) {
                            fakeNames.add(profile.getName());
                        }
                    }
                }
                return fakeNames.stream().filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
