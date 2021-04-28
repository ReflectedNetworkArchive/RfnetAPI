package network.reflected.rfnetapi;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class RfnetAPI extends JavaPlugin {
    ServerConfig serverConfig = new ServerConfig();
    Database database = new Database(serverConfig);

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
    }

    @Override
    public void onDisable() {
        database.setAvailable(false);
        database.close();
    }
}
