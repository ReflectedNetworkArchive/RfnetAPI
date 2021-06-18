package network.reflected.rfnetapi;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public final class RfnetAPI extends JavaPlugin implements Listener {
    private final ServerConfig serverConfig = new ServerConfig();
    private final Database database = new Database(serverConfig);
    @Getter private String loadedMap;

    @Override
    public void onEnable() {
        // If something is wrong with the config, shutdown the server, since it won't be connectable.
        if (!serverConfig.isValid()) {
            getLogger().log(Level.SEVERE, serverConfig.whatsMissing());
            getServer().shutdown();
        }

        // If the database isn't connected, its either having issues or not available.
        // Since this server won't be connectable, shut it down.
        if (!database.isConnected()) {
            getLogger().log(Level.SEVERE, "Could not connect to database! Look in console for errors.");
            getServer().shutdown();
        }

            // TODO: figure out how to make the API actually work.
//            // Load the worlds as defined in the config
//            // Start by setting up the SWM plugin
//            SlimePlugin slime = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
//            if (slime != null) {
//                slimeMongoLoader = slime.getLoader("mongodb");
//
//                // Now set some properties of the world
//                SlimePropertyMap mapProperties = new SlimePropertyMap();
//                mapProperties.setValue(SlimeProperties.DIFFICULTY, "normal");
//                mapProperties.setValue(SlimeProperties.SPAWN_X, 0);
//                mapProperties.setValue(SlimeProperties.SPAWN_Y, 64);
//                mapProperties.setValue(SlimeProperties.SPAWN_Z, 0);
//                mapProperties.setValue(SlimeProperties.WORLD_TYPE, "FLAT"); // removes void effect at lower y levels
//
//                // And finally, find out which world to load.
//                try {
//                    if (serverConfig.getMaps().size() == 1) { // There is only one map to choose
//                        // Also that magic boolean before mapProperties is whether it's read only. No slime worlds we load
//                        // would be good to make editable (they are all minigame maps)
//                        loadedMap = slime.loadWorld(slimeMongoLoader, serverConfig.getMaps().get(0), true, mapProperties);
//                    } else { // A map must be chosen at random.
//                        List<String> maps = serverConfig.getMaps();
//                        Random rand = new Random();
//                        // See above for what the magic boolean is
//                        loadedMap = slime.loadWorld(slimeMongoLoader, maps.get(rand.nextInt(maps.size())), true, mapProperties);
//                    }
//
//                    getLogger().info("Loading " + loadedMap.getName());
//                    slime.generateWorld(loadedMap);
//                } catch (Exception e) {
//                    // If the map fails to load, there's nothing to connect to, so stop the server.
//                    e.printStackTrace();
//                    getLogger().log(Level.SEVERE, "Error loading a map! The server has to shut down!");
//                    getServer().shutdown();
//                }
//            }

        // Setup a plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register this class as an event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register command events
        getServer().getPluginManager().registerEvents(ReflectedAPI.getCommandProvider(), this);

        // Setup default commands, available on every server
        DefaultCommands.initialize();

        // Some stuff should be run AFTER the server has fully loaded.
        getServer().getScheduler().runTaskLater(this, () -> {
            if (serverConfig.getMaps().size() == 1) { // There is only one map to choose
                loadedMap = serverConfig.getMaps().get(0);
            } else { // A map must be chosen at random.
                List<String> maps = serverConfig.getMaps();
                Random rand = new Random();
                loadedMap = maps.get(rand.nextInt(maps.size()));
            }
            // Run the command to load the right map. Janky, but it works.
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "swm load " + loadedMap);
            ReflectedAPI.setMapName(loadedMap);

            // Add this server's information to Redis for ServerDiscovery.
            // Args: Individual server ID, type of server, whether it's online & accepting players
            database.setAvailable(true);
        }, 1); // 1 tick, so waits until the server is fully started (started ticking)
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void playerJoin(PlayerJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        location.setWorld(Bukkit.getWorld(loadedMap));
        event.getPlayer().teleport(location);
    }
}
