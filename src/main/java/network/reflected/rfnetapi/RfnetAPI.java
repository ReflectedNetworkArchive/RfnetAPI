package network.reflected.rfnetapi;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class RfnetAPI extends JavaPlugin implements Listener {
    private final ServerConfig serverConfig = new ServerConfig();
    private final Database database = new Database(serverConfig);

    @Override
    public void onEnable() {
        // If something is wrong with the config, shutdown the server, since it won't be connectable.
        if (!serverConfig.isValid()) {
            getLogger().log(Level.SEVERE, serverConfig.whatsMissing());
            getServer().shutdown();
        }

        getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
            // If the database isn't connected by now, its either having issues or not available.
            // Since this server won't be connectable, shut it down.
            if (!database.isConnected()) {
                getLogger().log(Level.SEVERE, "Could not connect to database! Look in console for errors.");
                getServer().shutdown();
            }
        }, 100); // 5 seconds = 100 ticks

        // Add this server's information to Redis for ServerDiscovery.
        // Args: Individual server ID, type of server, whether it's online & accepting players
        database.setAvailable(true);

        // Setup a plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register this class as an event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Remove this server from the list of ones that are connectable
        database.setAvailable(false);
        // And then close the connections to the database
        // so we don't overload them.
        database.close();
    }

    // Sends a plugin message to ServerDiscovery running on bungee.
    public void sendPlayer(Player player, String archetype) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // See the spec for this in ServerDiscovery
        out.writeUTF("send:" + player.getUniqueId() + ":" + archetype);

        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }
}
