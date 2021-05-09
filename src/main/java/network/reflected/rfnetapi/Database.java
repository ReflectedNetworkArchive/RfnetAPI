package network.reflected.rfnetapi;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.bukkit.Bukkit;

import java.time.Duration;

// Abstracts away database so I can swap it out easier, since I
// would only have to change code in here. Also takes in a config
// to save the hassle of passing in a bunch of values that are
// just straight from the config anyways.
public class Database {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    private MongoDatabase database;
    private MongoClient mongoClient;
    private ServerConfig serverConfig;
    private boolean connected = false;

    public Database(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;

        // In a try-catch because sometimes it fails. In the case that it does, connected will not be set. Yay!
        try {
            // Mongo initialization
            ConnectionString connString = new ConnectionString(
                    serverConfig.getMongoURI()
            );
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .retryWrites(true)
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("purchasedata");

            // Lettuce initialization
            redisClient = RedisClient.create("");
            redisConnection = redisClient.connect(
                    new RedisURI(
                            serverConfig.getRedisURI(),
                            serverConfig.getRedisPort(),
                            Duration.ofSeconds(5)
                    )
            );

            // If the above has finished without error, we're connected!
            connected = false;
        } catch (Exception e) {
            e.printStackTrace(); // Don't just swallow the error that would be bad ;)
        }
    }

    // Getter.
    public boolean isConnected() {
        return connected;
    }

    // Registers or un-registers this server with ServerDiscovery
    public void setAvailable(boolean available) {
        if (available) {
            // Register this server by adding it to a set of server ids.
            redisConnection.sync().sadd("servers", serverConfig.getId());

            // Register the settings for this type of server, and this server as a valid archetype
            redisConnection.sync().sadd("archetypes", serverConfig.getArchetype());

            redisConnection.sync().hset(
                    "archetype:" + serverConfig.getArchetype(),
                    "strategy",
                    serverConfig.getConnectionStrategy()
            );

            redisConnection.sync().hset(
                    "archetype:" + serverConfig.getArchetype(),
                    "globalChat",
                    String.valueOf(serverConfig.isGlobalChatEnabled())
            );

            // Creates a hash map with a name of server:<id> and fills it with
            // information about this instance.
            redisConnection.sync().hset(
                    "server:" + serverConfig.getId(),
                    "archetype",
                    serverConfig.getArchetype()
            );

            redisConnection.sync().hset(
                    "server:" + serverConfig.getId(),
                    "players",
                    "0"
            );

            redisConnection.sync().hset(
                    "server:" + serverConfig.getId(),
                    "maxplayers",
                    String.valueOf(Bukkit.getMaxPlayers()) // Sadly with this redis driver I must convert to string
            );

            redisConnection.sync().hset(
                    "server:" + serverConfig.getId(),
                    "address",
                    serverConfig.getAddress()
            );
        } else {
            // Un-register this server. The metadata is left in for speed, since it'll probably
            // be reused when this server restarts.
            redisConnection.sync().srem("servers", serverConfig.getId());
        }
    }

    // Updates the player count so that the bungee doesn't send
    // too many and cause them to get kicked.
    public void updatePlayerCount() {
        redisConnection.sync().hset(
                "server:" + serverConfig.getId(),
                "players",
                String.valueOf(Bukkit.getOnlinePlayers().size())
        );
    }

    // Shutdown database stuff. Please call this fn or me and the databases will cry.
    public void close() {
        redisConnection.close();
        redisClient.shutdown();
        mongoClient.close();
    }
}
