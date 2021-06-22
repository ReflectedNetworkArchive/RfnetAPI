package network.reflected.rfnetapi;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.SWMPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public final class RfnetAPI extends JavaPlugin implements Listener {
    @Getter private ReflectedAPI api;
    @Getter private final ServerConfig serverConfig = new ServerConfig();
    @Getter private final Database database = new Database(serverConfig);
    @Getter private String loadedMapName = "world";
    @Getter private boolean minigameWorld = false;

    @Override
    public void onEnable() {
        // Init the API and let any waiting plugins know that it's ready now.
        api = new ReflectedAPI(this);
        ReflectedAPI.checkCallbacks();

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

        // Setup a plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register this class as an event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register other API's events
        ReflectedAPI.get(api -> {
            getServer().getPluginManager().registerEvents(api.getCommandProvider(), this);
            getServer().getPluginManager().registerEvents(api.getPurchaseAPI(), this);
        });

        // Setup default commands, available on every server
        DefaultCommands.initialize();

        // Some stuff should be run AFTER the server has fully loaded.
        getServer().getScheduler().runTaskLater(this, () -> {
            // Load the worlds as defined in the config
            // Start by setting up the SWM plugin
            getLogger().info("Getting SWM plugin.");
            try {
                SWMPlugin slime = SWMPlugin.getInstance();
                if (slime != null) { // If slime plugin can't be found, we're not on a minigame server.
                    getLogger().info("Configuring worlds...");
                    SlimeLoader slimeMongoLoader = slime.getLoader("mongodb");

                    // Now set some properties of the world
                    SlimePropertyMap mapProperties = new SlimePropertyMap();
                    mapProperties.setValue(SlimeProperties.DIFFICULTY, "normal");
                    mapProperties.setValue(SlimeProperties.SPAWN_X, 0);
                    mapProperties.setValue(SlimeProperties.SPAWN_Y, 64);
                    mapProperties.setValue(SlimeProperties.SPAWN_Z, 0);
                    mapProperties.setValue(SlimeProperties.WORLD_TYPE, "FLAT"); // removes void effect at lower y levels

                    // And finally, find out which world to load.
                    getLogger().info("Looking for worlds to load.");
                    try {
                        SlimeWorld loadedMap;
                        if (serverConfig.getMaps().size() == 1) { // There is only one map to choose
                            // Also that magic boolean before mapProperties is whether it's read only. No slime worlds we load
                            // would be good to make editable (they are all minigame maps)
                            loadedMap = slime.loadWorld(slimeMongoLoader, serverConfig.getMaps().get(0), true, mapProperties);
                        } else { // A map must be chosen at random.
                            List<String> maps = serverConfig.getMaps();
                            Random rand = new Random();
                            // See above for what the magic boolean is
                            loadedMap = slime.loadWorld(slimeMongoLoader, maps.get(rand.nextInt(maps.size())), true, mapProperties);
                        }

                        getLogger().info("Loading " + loadedMap.getName());
                        slime.generateWorld(loadedMap);
                        loadedMapName = loadedMap.getName();
                        minigameWorld = true;
                    } catch (Exception e) {
                        // If the map fails to load, there's nothing to connect to, so stop the server.
                        e.printStackTrace();
                        getLogger().log(Level.SEVERE, "Error loading a map! The server has to shut down!");
                        getServer().shutdown();
                    }
                }
            } catch (Exception e) {
                getLogger().info("An error occured when attempting to load a world, so minigame world support has been disabled.");
            }

            // Add this server's information to Redis for ServerDiscovery.
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
        if (!api.getPurchaseAPI().isAuthenticating(player) || archetype.equals("lobby")) {  // If the player is busy making a purchase or changing a password, don't interrupt it.
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            // See the spec for this in ServerDiscovery
            out.writeUTF("send:" + player.getUniqueId() + ":" + archetype);

            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void playerJoin(PlayerJoinEvent event) {
        if (isMinigameWorld()) {
            Location location = event.getPlayer().getLocation();
            location.setWorld(Bukkit.getWorld(loadedMapName));
            event.getPlayer().teleport(location);
        }
    }


    public void restart() {
        ReflectedAPI.get().setAvailable(false);

        // Send everybody to another server
        for (Player player : Bukkit.getOnlinePlayers()) {
            ReflectedAPI.get().sendPlayer(player, ReflectedAPI.get().getPlugin().getServerConfig().getArchetype());
        }

        // Wait one second so players don't get Server Closed before being sent back to lobby
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new Thread(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder("nohup", "sh", "restart.sh");
                try {
                    processBuilder.directory(new File("."));
                    processBuilder.redirectErrorStream(false);
                    processBuilder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            Bukkit.shutdown();
        }, 20);
    }
}
