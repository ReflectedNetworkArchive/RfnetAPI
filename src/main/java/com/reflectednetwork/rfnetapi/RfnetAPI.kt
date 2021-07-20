package com.reflectednetwork.rfnetapi

import com.google.common.io.ByteStreams
import com.grinderwolf.swm.api.world.SlimeWorld
import com.grinderwolf.swm.api.world.properties.SlimeProperties
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import com.grinderwolf.swm.plugin.SWMPlugin
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import com.reflectednetwork.rfnetapi.medallions.MedallionAPI
import com.reflectednetwork.rfnetapi.purchases.PurchaseEvents
import com.reflectednetwork.rfnetapi.purchases.PurchaseGUI
import org.apache.commons.io.IOUtils
import org.bstats.bukkit.Metrics
import org.bstats.charts.MultiLineChart
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.logging.Level
import kotlin.math.roundToInt

class RfnetAPI : JavaPlugin(), Listener {
    val ver = 23 // The current version
    private var disabledForUpdate = false

    var api: ReflectedAPI? = null
    val serverConfig = ServerConfig()
    val database = Database(serverConfig)
    var loadedMap: SlimeWorld? = null
    var minigameWorld = false

    override fun onEnable() {
        try {
            // Init the API and let any waiting plugins know that it's ready now.
            api = ReflectedAPI(this)

            // If something is wrong with the config, shutdown the server, since it won't be connectable.
            if (!serverConfig.isValid()) {
                logger.log(Level.SEVERE, serverConfig.whatsMissing())
                server.shutdown()
            }

            // Setup a plugin messaging channel
            server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

            // Register this class as an event listener
            server.pluginManager.registerEvents(this, this)

            // Register other API's events
            server.pluginManager.registerEvents(ReflectedAPI.get().commandProvider, this)
            server.pluginManager.registerEvents(ReflectedAPI.get().purchaseAPI, this)

            server.pluginManager.registerEvents(PurchaseGUI, this)
            server.pluginManager.registerEvents(PurchaseEvents, this)
            server.pluginManager.registerEvents(ExceptionDispensary, this)
            server.pluginManager.registerEvents(MedallionAPI, this)

            // Check online receipts on occasion. Runs async so it isn't *too* expensive
            // Well, the possible performance drop is worth convenience for buyers
            server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                for (player in server.onlinePlayers) {
                    PurchaseEvents.checkReceipts(player)
                }
            }, 400, 400)

            // Setup default commands, available on every server
            DefaultCommands.initialize()

            // Some stuff should be run AFTER the server has fully loaded.
            server.scheduler.runTaskLater(this, Runnable {

                // Load the worlds as defined in the config
                // Start by setting up the SWM plugin
                logger.info("Getting SWM plugin.")
                try {
                    if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") == null) {
                        throw NoClassDefFoundError()
                    }
                    val slime = SWMPlugin.getInstance()
                    if (slime != null) { // If slime plugin can't be found, we're not on a minigame server.
                        logger.info("Configuring worlds...")
                        val slimeMongoLoader = slime.getLoader("mongodb")

                        // Now set some properties of the world
                        val mapProperties = SlimePropertyMap()
                        mapProperties.setValue(SlimeProperties.DIFFICULTY, "normal")
                        mapProperties.setValue(SlimeProperties.SPAWN_X, 0)
                        mapProperties.setValue(SlimeProperties.SPAWN_Y, 64)
                        mapProperties.setValue(SlimeProperties.SPAWN_Z, 0)
                        mapProperties.setValue(
                            SlimeProperties.WORLD_TYPE,
                            "FLAT"
                        ) // removes void effect at lower y levels

                        // And finally, find out which world to load.
                        logger.info("Looking for worlds to load.")
                        try {
                            loadedMap = if (serverConfig.maps.size == 1) {
                                // There is only one map to choose
                                // Also that magic boolean before mapProperties is whether it's read only. No slime worlds we load
                                // would be good to make editable (they are all minigame maps)
                                slime.loadWorld(slimeMongoLoader, serverConfig.maps[0], true, mapProperties)
                            } else { // A map must be chosen at random.
                                val maps = serverConfig.maps
                                val rand = Random()
                                // See above for what the magic boolean is
                                slime.loadWorld(
                                    slimeMongoLoader,
                                    maps[rand.nextInt(maps.size)],
                                    true,
                                    mapProperties
                                )
                            }
                            logger.info("Loading ${loadedMap?.name}")
                            slime.generateWorld(loadedMap)
                            minigameWorld = true
                        } catch (e: Exception) {
                            // If the map fails to load, there's nothing to connect to, so stop the server.
                            ExceptionDispensary.report(e, "loading map")
                            logger.log(Level.SEVERE, "Error loading a map! The server has to shut down!")
                            server.shutdown()
                        }
                    }
                } catch (e: NoClassDefFoundError) {
                    logger.info("An error occured when attempting to load SWM, so minigame world support has been disabled.")
                }

                // Add this server's information to Redis for ServerDiscovery.
                database.setAvailable(true)
            }, 1) // 1 tick, so waits until the server is fully started (started ticking)

            // Stats
            val metrics = Metrics(this, 12072)
            server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                metrics.addCustomChart(MultiLineChart("mspt") {
                    mapOf(
                        Pair(
                            serverConfig.id,
                            server.averageTickTime.roundToInt()
                        )
                    )
                })
            }, 20, 20)

            restart() // TODO: Undo this! This is a fake updater update
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "enabling plugin")
        }
    }

    override fun onDisable() {
        try {
            if (!disabledForUpdate) {
                updateCheck()
                // Remove this server from the list of ones that are connectable
                database.setAvailable(false)
                database.updatePlayerCount(0)
                // And then close the connections to the database
                // so we don't overload them.
                database.close()
            }
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "disabling plugin")
        }
    }

    // Sends a plugin message to ServerDiscovery running on bungee.
    fun sendPlayer(player: Player, archetype: String) {
        try {
            val out = ByteStreams.newDataOutput()

            // See the spec for this in ServerDiscovery
            out.writeUTF("send:" + player.uniqueId + ":" + archetype)
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray())
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "sending player")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun playerJoin(event: PlayerJoinEvent) {
        try {
            if (minigameWorld) {
                loadedMap?.let {
                    val location = event.player.location
                    location.world = Bukkit.getWorld(it.name)
                    event.player.teleport(location)
                }
            }

            database.updatePlayerCount(Bukkit.getOnlinePlayers().size)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "player join")
        }
    }

    @EventHandler
    private fun playerQuit(event: PlayerQuitEvent) {
        try {
            database.updatePlayerCount(Bukkit.getOnlinePlayers().size - 1)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "player quit")
        }
    }

    fun restart() {
        try {
            disabledForUpdate = true

            // Remove this server from the list of ones that are connectable
            database.setAvailable(false)
            database.updatePlayerCount(0)

            // Send everybody to another server
            for (player in Bukkit.getOnlinePlayers()) {
                sendPlayer(player, serverConfig.archetype)
            }
            updateCheck()

            // And then close the connections to the database
            // so we don't overload them.
            database.close()

            // Wait one second so players don't get Server Closed before being sent back to lobby
            Bukkit.getScheduler().runTaskLater(this, Runnable {
                try {
                    val runtime = Runtime.getRuntime()
                    runtime.addShutdownHook(Thread {
                        val processBuilder = ProcessBuilder("nohup", "sh", "restart.sh")
                        try {
                            processBuilder.directory(File("."))
                            processBuilder.redirectErrorStream(false)
                            processBuilder.start()
                        } catch (e: IOException) {
                            ExceptionDispensary.report(e, "restarting")
                        }
                    })
                    Bukkit.shutdown()
                } catch (e: Exception) {
                    ExceptionDispensary.report(e, "shutting down for restart")
                }
            }, 20)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "restarting")
        }
    }

    private fun updateCheck() {
        try {
            println("Checking for updates...")
            // Check for updates
            val nextVer = ver + 1
            // Credentials for a read-only user
            val username = "31faf87b-0584-449b-b5b4-542b711fedfd"
            val password =
                "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiIzMWZhZjg3Yi0wNTg0LTQ0OWItYjViNC01NDJiNzExZmVkZmQiLCJhdWQiOiIzMWZhZjg3Yi0wNTg0LTQ0OWItYjViNC01NDJiNzExZmVkZmQiLCJvcmdEb21haW4iOiJyZWZsZWN0ZWRuZXR3b3JrIiwibmFtZSI6InNlcnZlci11cGRhdGVyIiwiaXNzIjoiaHR0cHM6XC9cL2pldGJyYWlucy5zcGFjZSIsInBlcm1fdG9rZW4iOiIzZE9ueW4zVjY4U0oiLCJwcmluY2lwYWxfdHlwZSI6IlNFUlZJQ0UiLCJpYXQiOjE2MjQ0Nzk3MDB9.BmoDM8WFtadwvF9506wpDugq7yNtaiCyWIzfX-cC4dGYSQgtTJrISH85fYI0ukD8E4_xFN74xmwa6Pc1gI6kCYICFkk1YIREsdAS08m4KCtAM4iK5YFAD2aodlIOgGVVon7EEqTM8KHBCjIyEVk9R-Vj8tLi37j4cnubfo7VkMk"
            val download = File("./plugins/RfnetAPI-$nextVer.jar")
            try {
                val downloadStream = FileOutputStream(download)
                val basicAuthenticationEncoded =
                    Base64.getEncoder().encodeToString("$username:$password".toByteArray(charset("UTF-8")))
                val url =
                    URL("https://maven.pkg.jetbrains.space/reflectednetwork/p/internalapi/maven/com/reflectednetwork/RfnetAPI/$nextVer/RfnetAPI-$nextVer.jar")
                val urlConnection = url.openConnection()
                urlConnection.setRequestProperty("Authorization", "Basic $basicAuthenticationEncoded")
                IOUtils.copy(urlConnection.getInputStream(), downloadStream)
                File("./plugins/RfnetAPI-$ver.jar").deleteOnExit()
                println("Update complete!")
            }  catch (e: FileNotFoundException) {
                println("No update found!")
                download.delete()
            } catch (e: IOException) { // We got a 404 meaning the file doesn't exist
                if (e.message?.contains("404") != true) throw e
                println("No update found!")
                download.delete()
            }
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "updating")
        }
    }
}