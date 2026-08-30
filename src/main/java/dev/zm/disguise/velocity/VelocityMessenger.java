package dev.zm.disguise.velocity;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.zm.disguise.models.DisguiseProfile;
import dev.zm.disguise.zMDisguise;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class VelocityMessenger implements PluginMessageListener {

    public static final String CHANNEL_BUNGEE = "BungeeCord";

    public static final String SUB_DISGUISE = "zMDisguise:disguise";
    public static final String SUB_UNDISGUISE = "zMDisguise:undisguise";

    private final zMDisguise plugin;
    private final Logger log;

    public VelocityMessenger(zMDisguise plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_BUNGEE);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_BUNGEE, this);
        log.info("[VelocityMessenger] Plugin messaging channel registered: " + CHANNEL_BUNGEE);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_BUNGEE);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_BUNGEE, this);
    }

    public void sendDisguise(Player player, DisguiseProfile profile) {
        if (!isProxied(player))
            return;

        String fakeName = profile.getName() != null ? profile.getName() : player.getName();
        String texValue = "";
        String texSig = "";

        PlayerProfile pp = player.getPlayerProfile();
        for (ProfileProperty prop : pp.getProperties()) {
            if ("textures".equals(prop.getName())) {
                texValue = prop.getValue();
                texSig = prop.getSignature() != null ? prop.getSignature() : "";
                break;
            }
        }

        byte[] payload = buildPayload(SUB_DISGUISE, fakeName, texValue, texSig);
        if (payload != null) {
            player.sendPluginMessage(plugin, CHANNEL_BUNGEE, payload);
            log.fine("[VelocityMessenger] Sent disguise for " + player.getName() + " → " + fakeName);
        }
    }

    public void sendUndisguise(Player player) {
        if (!isProxied(player))
            return;

        String realName = player.getName();
        String texValue = "";
        String texSig = "";

        PlayerProfile pp = player.getPlayerProfile();
        for (ProfileProperty prop : pp.getProperties()) {
            if ("textures".equals(prop.getName())) {
                texValue = prop.getValue();
                texSig = prop.getSignature() != null ? prop.getSignature() : "";
                break;
            }
        }

        byte[] payload = buildPayload(SUB_UNDISGUISE, realName, texValue, texSig);
        if (payload != null) {
            player.sendPluginMessage(plugin, CHANNEL_BUNGEE, payload);
            log.fine("[VelocityMessenger] Sent undisguise for " + player.getName());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL_BUNGEE.equals(channel))
            return;
    }

    private boolean isProxied(Player player) {
        return true;
    }

    private byte[] buildPayload(String subChannel, String name, String texValue, String texSig) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos)) {

            dos.writeUTF(subChannel);
            dos.writeUTF(name);
            dos.writeUTF(texValue);
            dos.writeUTF(texSig);
            return bos.toByteArray();

        } catch (IOException e) {
            log.warning("[VelocityMessenger] Failed to build payload: " + e.getMessage());
            return null;
        }
    }
}
