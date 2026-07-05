package dev.zm.disguise.commands;

import dev.zm.disguise.disguise.DisguiseRemover;
import dev.zm.disguise.disguise.DisguiseService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnDisguiseCommand implements CommandExecutor, TabCompleter {

    private final zMDisguise plugin;
    private final DisguiseRemover disguiseRemover;
    private final DisguiseService disguiseService;

    public UnDisguiseCommand(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseRemover = plugin.getDisguiseRemover();
        this.disguiseService = plugin.getDisguiseService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessageManager().get("messages.only-players"));
                return true;
            }
            return undisguiseSelf(player);
        }

        String targetName = args[0];

        if (sender instanceof Player senderPlayer) {
            if (!senderPlayer.hasPermission(plugin.getSettingsManager().getPermissionAdmin())) {
                senderPlayer.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
                return true;
            }
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().get("messages.target-not-online",
                    "name", targetName));
            return true;
        }

        if (!disguiseService.isDisguised(target)) {
            sender.sendMessage(plugin.getMessageManager().get("messages.not-disguised"));
            return true;
        }

        disguiseRemover.removeDisguise(target);
        sender.sendMessage(plugin.getMessageManager().get("messages.undisguised-other",
                "real_name", target.getName()));
        target.sendMessage(plugin.getMessageManager().get("messages.undisguised-success"));
        return true;
    }

    private boolean undisguiseSelf(Player player) {
        if (!player.hasPermission(plugin.getSettingsManager().getPermissionUse())) {
            player.sendMessage(plugin.getMessageManager().get("messages.no-permission"));
            return true;
        }
        if (!disguiseService.isDisguised(player)) {
            player.sendMessage(plugin.getMessageManager().get("messages.not-disguised"));
            return true;
        }
        disguiseRemover.removeDisguise(player);
        player.sendMessage(plugin.getMessageManager().get("messages.undisguised-success"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            boolean isAdmin = !(sender instanceof Player p)
                    || p.hasPermission(plugin.getSettingsManager().getPermissionAdmin());

            if (!isAdmin)
                return List.of();

            String partial = args[0].toLowerCase();
            return disguiseService.getActiveDisguises().entrySet().stream()
                    .map(Map.Entry::getKey)
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
