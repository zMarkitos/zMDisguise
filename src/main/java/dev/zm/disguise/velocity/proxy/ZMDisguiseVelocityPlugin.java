package dev.zm.disguise.velocity.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

@Plugin(id = "zmdisguise", name = "zMDisguise", version = "1.1.0", authors = { "zMarkitos_" })
public class ZMDisguiseVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;

    private static final MinecraftChannelIdentifier BUNGEE_CHANNEL = MinecraftChannelIdentifier.from("bungeecord:main");

    @Inject
    public ZMDisguiseVelocityPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(BUNGEE_CHANNEL);
        logger.info("zMDisguise Velocity bridge has been initialized! Listening for disguise sync messages...");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(BUNGEE_CHANNEL)) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String subChannel = in.readUTF();

            if (subChannel.equals("zMDisguise:disguise")) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());

                String fakeName = in.readUTF();
                String texValue = in.readUTF();
                String texSig = in.readUTF();

                if (event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection) {
                    com.velocitypowered.api.proxy.ServerConnection backend = (com.velocitypowered.api.proxy.ServerConnection) event
                            .getSource();
                    Player player = backend.getPlayer();
                    logger.debug("Received disguise sync for " + player.getUsername() + " -> " + fakeName);

                }

            } else if (subChannel.equals("zMDisguise:undisguise")) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());

                String realName = in.readUTF();
                String texValue = in.readUTF();
                String texSig = in.readUTF();

                if (event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection) {
                    com.velocitypowered.api.proxy.ServerConnection backend = (com.velocitypowered.api.proxy.ServerConnection) event
                            .getSource();
                    Player player = backend.getPlayer();
                    logger.debug("Received undisguise sync for " + player.getUsername());
                }
            }
        } catch (IOException e) {
        }
    }
}
