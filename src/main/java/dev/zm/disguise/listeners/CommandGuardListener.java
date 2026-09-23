package dev.zm.disguise.listeners;

import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A disguised player answers to their fake name everywhere, including /tp. That would let anyone
 * teleport "to the account owner" and land on the disguised player instead. Commands listed under
 * {@code hide-from-commands} get a plain "player not found" when a non-self argument is a disguise
 * name; senders with the bypass permission are exempt.
 */
public class CommandGuardListener implements Listener {

    private final zMDisguise plugin;

    public CommandGuardListener(zMDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Set<String> guarded = new HashSet<>();
        for (String c : plugin.getSettingsManager().hideFromCommands())
            guarded.add(c.toLowerCase(Locale.ROOT));
        if (guarded.isEmpty())
            return;

        String[] parts = event.getMessage().substring(1).trim().split("\\s+");
        if (parts.length < 2)
            return;
        String label = parts[0].toLowerCase(Locale.ROOT);
        int ns = label.indexOf(':');
        if (ns >= 0)
            label = label.substring(ns + 1); // essentials:tp -> tp
        if (!guarded.contains(label))
            return;

        Player sender = event.getPlayer();
        if (sender.hasPermission(plugin.getSettingsManager().getPermissionBypass()))
            return;

        for (int i = 1; i < parts.length; i++) {
            String arg = parts[i];
            int colon = arg.indexOf(':');
            if (colon > 0)
                arg = arg.substring(0, colon); // /home <player>:<home>
            Player hit = plugin.getNameResolver().findByFakeName(arg).orElse(null);
            if (hit != null && !hit.equals(sender)) {
                event.setCancelled(true);
                sender.sendMessage(plugin.getMessageManager().get("messages.player-not-found", "name", parts[i]));
                return;
            }
        }
    }
}
