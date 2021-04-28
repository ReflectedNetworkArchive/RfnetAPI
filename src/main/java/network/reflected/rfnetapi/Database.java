package network.reflected.rfnetapi;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.bukkit.Bukkit;

public class Database {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    private MongoDatabase database;
    private MongoClient mongoClient;
    private ServerConfig serverConfig;
    private boolean connected = false;

    public Database(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;

        try {
            ConnectionString connString = new ConnectionString(
                    serverConfig.getMongoURI()
            );
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .retryWrites(true)
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("purchasedata");
            redisClient = RedisClient.create("");
            redisConnection = redisClient.connect(serverConfig.getRedisURI());
            connected = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    // Registers or un-registers this server with ServerDiscovery
    public void setAvailable(boolean available) {
        if (available) {
            // Register this server by adding it to a set of server ids.
            redisConnection.sync().sadd("servers", serverConfig.getId());

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
        } else {
            // Un-register this server. The metadata is left in for speed, since it'll probably
            // be reused when this server restarts.
            redisConnection.sync().srem("servers", serverConfig.getId());
        }
    }

    public void updatePlayerCount() {
        redisConnection.sync().hset(
                "server:" + serverConfig.getId(),
                "players",
                String.valueOf(Bukkit.getOnlinePlayers().size())
        );
    }

    public void close() {
        redisConnection.close();
        redisClient.shutdown();
        mongoClient.close();
    }
}
