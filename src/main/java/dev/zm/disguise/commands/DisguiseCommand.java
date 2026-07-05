package dev.zm.disguise.commands;

import dev.zm.disguise.disguise.NameResolver;
import dev.zm.disguise.gui.MainMenu;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DisguiseCommand implements CommandExecutor, TabCompleter {

    private static final String NAME_PATTERN = "[a-zA-Z0-9_]{1,16}";

    private final zMDisguise plugin;

    public DisguiseCommand(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessageManager().get("messages.only-players"));
                return true;
            }
            if (!player.hasPermission(plugin.getSettingsManager().getPermissionUse())) {
                player.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }
            new MainMenu(plugin).open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessageManager().get("messages.only-players"));
                return true;
            }
            return handleSave(player, args);
        }

        return handleDisguise(sender, args);
    }

    private boolean handleSave(Player player, String[] args) {
        if (!player.hasPermission(plugin.getSettingsManager().getPermissionUse())) {
            player.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().colorize("&#FF4444Uso: /disguise save <nombre>"));
            return true;
        }
        if (!plugin.getDisguiseService().isDisguised(player)) {
            player.sendMessage(plugin.getMessageManager().get("messages.must-be-disguised"));
            return true;
        }
        String saveName = args[1];
        if (!saveName.matches(NAME_PATTERN)) {
            player.sendMessage(plugin.getMessageManager().colorize("&#FF4444Nombre inválido."));
            return true;
        }
        DisguiseProfile active = plugin.getDisguiseService().getDisguise(player);

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStorageService().saveSavedProfile(player.getUniqueId(), saveName, active);
            player.sendMessage(plugin.getMessageManager().get("messages.saved-success", "name", saveName));
        });
        return true;
    }

    private boolean handleDisguise(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player
                    && !player.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                sender.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }
            for (net.kyori.adventure.text.Component line : plugin.getMessageManager().getList("messages.help")) {
                sender.sendMessage(line);
            }
            return true;
        }

        String targetName = args[0];
        String fakeName = args[1];

        Player target;
        boolean isPlayerSender = sender instanceof Player;
        boolean disguisingSelf = isPlayerSender && targetName.equalsIgnoreCase(sender.getName());

        if (disguisingSelf) {
            target = (Player) sender;
        } else {
            if (isPlayerSender && !sender.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                sender.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getMessageManager().get("messages.target-not-online", "name", targetName));
                return true;
            }
        }

        if (disguisingSelf && !sender.hasPermission(plugin.getSettingsManager().getPermissionUse())) {
            sender.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
            return true;
        }

        if (!fakeName.matches(NAME_PATTERN)) {
            sender.sendMessage(plugin.getMessageManager().get("messages.player-not-found", "name", fakeName));
            return true;
        }

        NameResolver resolver = plugin.getNameResolver();
        if (resolver.isNameTaken(fakeName, target)) {
            sender.sendMessage(plugin.getMessageManager().get("messages.name-taken", "name", fakeName));
            return true;
        }

        buildAndApplyDisguise(sender, target, fakeName, disguisingSelf);
        return true;
    }

    // Builds the disguise profile, resolving LuckPerms group data for the
    // fake name when possible, then applies it back on the main thread.
    private void buildAndApplyDisguise(CommandSender sender, Player target, String fakeName, boolean disguisingSelf) {
        dev.zm.disguise.hooks.LuckPermsHook lp = plugin.getHookManager().getLuckPermsHook();

        if (lp == null || !lp.isEnabled()) {
            DisguiseProfile profile = new DisguiseProfile(target.getUniqueId());
            profile.setName(fakeName);
            profile.setSkin(fakeName);
            finalizeDisguise(sender, target, fakeName, profile, disguisingSelf);
            return;
        }

        // LuckPerms lookups and offline player scans can be slow, run off the main
        // thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DisguiseProfile profile = new DisguiseProfile(target.getUniqueId());
            profile.setName(fakeName);
            profile.setSkin(fakeName);

            org.bukkit.OfflinePlayer source = resolveOfflinePlayerByName(fakeName);
            if (source != null) {
                List<String> groups = lp.getAllGroupsAsync(source.getUniqueId()).join();
                if (groups != null && !groups.isEmpty()) {
                    profile.setGroups(groups);
                    String primary = lp.getPrimaryGroupAsync(source.getUniqueId()).join();
                    if (primary != null && !primary.isEmpty()) {
                        profile.setPrimaryGroup(primary);
                        profile.setPrefix(lp.getPrefix(primary));
                        profile.setSuffix(lp.getSuffix(primary));
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!target.isOnline())
                    return; // target left while we were resolving groups
                finalizeDisguise(sender, target, fakeName, profile, disguisingSelf);
            });
        });
    }

    // Tries to match the fake name against an online or previously known offline
    // player,
    // the same way the skin lookup already treats the fake name as a real account
    // name.
    private org.bukkit.OfflinePlayer resolveOfflinePlayerByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null)
            return online;
        for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(op.getName())) {
                return op;
            }
        }
        return null;
    }

    private void finalizeDisguise(CommandSender sender, Player target, String fakeName, DisguiseProfile profile,
            boolean disguisingSelf) {
        plugin.getDisguiseApplier().applyDisguise(target, profile);

        if (disguisingSelf) {
            sender.sendMessage(plugin.getMessageManager().get(
                    "messages.disguised-success", "name", fakeName, "prefix", ""));
        } else {
            sender.sendMessage(plugin.getMessageManager().get(
                    "messages.disguised-other", "real_name", target.getName(), "name", fakeName));
            target.sendMessage(plugin.getMessageManager().get(
                    "messages.disguised-success", "name", fakeName, "prefix", ""));
        }

        if (sender instanceof Player p) {
            notifyAdmins(p, target, fakeName);
        } else {
            notifyAdmins(null, target, fakeName);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return List.of();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> options = new ArrayList<>();

            // "save" subcommand
            if (player.hasPermission(plugin.getSettingsManager().getPermissionUse())) {
                options.add("save");
            }
            // Online player names as targets (admins can target others; users target
            // themselves)
            if (player.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .forEach(options::add);
            } else {
                options.add(player.getName());
            }

            return options.stream()
                    .filter(o -> o.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
            // No suggestions for save name — free-form input
            return List.of();
        }

        if (args.length == 2) {
            // Second arg is the fake name — no suggestions to avoid revealing disguise
            // names
            return List.of();
        }

        return List.of();
    }

    private void notifyAdmins(Player sender, Player target, String fakeName) {
        if (!plugin.getSettingsManager().notifyAdmins())
            return;
        String perm = plugin.getSettingsManager().getPermissionNotify();
        net.kyori.adventure.text.Component msg = plugin.getMessageManager().get(
                "messages.admin-notify", "real_name", target.getName(), "name", fakeName);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(perm) && !p.equals(sender)) {
                p.sendMessage(msg);
            }
        }
    }
}
