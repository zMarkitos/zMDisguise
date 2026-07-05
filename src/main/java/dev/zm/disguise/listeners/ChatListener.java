package dev.zm.disguise.listeners;

import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Sobreescribe el nombre en el chat con el nombre del disfraz.
 */
public class ChatListener implements Listener {

    private final zMDisguise plugin;
    private final DisguiseService disguiseService;

    public ChatListener(zMDisguise plugin) {
        this.plugin = plugin;
        this.disguiseService = plugin.getDisguiseService();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!disguiseService.isDisguised(player))
            return;

        DisguiseProfile profile = disguiseService.getDisguise(player);

        // Build a new display name component from the disguise
        String nameRaw = (profile.getPrefix() != null ? profile.getPrefix() : "") + profile.getName();
        Component displayName = MessageManager.colorize(nameRaw);

        // Override the player display name for this event
        event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                .append(displayName)
                .append(Component.text(": "))
                .append(message)
                .build());
    }
}
