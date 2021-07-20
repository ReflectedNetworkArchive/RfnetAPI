package com.reflectednetwork.rfnetapi

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.bson.Document
import org.bukkit.Bukkit
import java.util.*

// Abstracts away database so I can swap it out easier, since I
// would only have to change code in here. Also takes in a config
// to save the hassle of passing in a bunch of values that are
// just straight from the config anyways.
class Database(private val serverConfig: ServerConfig) {
    private val redisClient = RedisClient.create()
    private val redisConnection = redisClient.connect(
        RedisURI.create(serverConfig.redisURI)
    )
    private val mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(serverConfig.mongoURI))
            .retryWrites(true)
            .build()
    )

    private var databaseClosed = false
    private var available = false

    // Registers or un-registers this server with ServerDiscovery
    fun setAvailable(available: Boolean) {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            this.available = available
            if (available) {
                // Register this server by adding it to a set of server ids.
                redisConnection.sync().sadd("servers", serverConfig.id)
                redisConnection.sync().sadd("allservers", serverConfig.id)

                // Register the settings for this type of server, and this server as a valid archetype
                redisConnection.sync().sadd("archetypes", serverConfig.archetype)
                redisConnection.sync().hset(
                    "archetype:" + serverConfig.archetype,
                    "strategy",
                    serverConfig.connectionStrategy
                )
                redisConnection.sync().hset(
                    "archetype:" + serverConfig.archetype,
                    "globalChat", serverConfig.isGlobalChatEnabled.toString()
                )

                // Creates a hash map with a name of server:<id> and fills it with
                // information about this instance.
                redisConnection.sync().hset(
                    "server:" + serverConfig.id,
                    "archetype",
                    serverConfig.archetype
                )
                redisConnection.sync().hset(
                    "server:" + serverConfig.id,
                    "players", Bukkit.getOnlinePlayers().size.toString()
                )
                redisConnection.sync().hset(
                    "server:" + serverConfig.id,
                    "maxplayers", Bukkit.getMaxPlayers().toString()
                )
                redisConnection.sync().hset(
                    "server:" + serverConfig.id,
                    "address",
                    serverConfig.address
                )
            } else {
                // Un-register this server. The metadata is left in for speed, since it'll probably
                // be reused when this server restarts.
                redisConnection.sync().srem("servers", serverConfig.id)
            }
        }
    }

    // Updates the player count so that the bungee doesn't send
    // too many and cause them to get kicked.
    fun updatePlayerCount(playerct: Int) {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            redisConnection.sync().hset(
                "server:" + serverConfig.id,
                "players", playerct.toString()
            )
        }
    }

    fun getTotalPlayercount(archetype: String): Int {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            val servers = redisConnection.sync().smembers("allservers")
            var players = 0
            for (serverId in servers) {
                if (redisConnection.sync().hget("server:$serverId", "archetype") == archetype) {
                    players += redisConnection.sync().hget("server:$serverId", "players").toInt()
                }
            }
            return players
        }
    }

    fun getBusyChangingPwd(player: UUID): Boolean {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            return redisConnection.sync()["changingpassword-$player"].toBoolean()
        }
    }

    fun setBusyChangingPwd(player: UUID, isBusy: Boolean) {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            redisConnection.sync()["changingpassword-$player"] = isBusy.toString()
        }
    }

    // Useful for api stuffs
    fun getCollection(databaseName: String, collectionName: String): MongoCollection<Document?> {
        if (databaseClosed) {
            throw NullPointerException("Database closed already!")
        } else {
            val db = mongoClient.getDatabase(databaseName)
            return try {
                db.getCollection(collectionName)
            } catch (e: IllegalArgumentException) {
                db.createCollection(collectionName)
                db.getCollection(collectionName)
            }
        }
    }

    // Shutdown database stuff. Please call this fn or me and the databases will cry.
    fun close() {
        if (!databaseClosed) {
            databaseClosed = true
            redisConnection.close()
            redisClient.shutdown()
            mongoClient.close()
        }
    }
}