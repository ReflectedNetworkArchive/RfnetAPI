package com.reflectednetwork.rfnetapi

import com.google.common.io.ByteStreams
import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import com.reflectednetwork.rfnetapi.loginstreaks.LoginStreakEvents
import com.reflectednetwork.rfnetapi.medallions.MedallionAPI
import com.reflectednetwork.rfnetapi.modtools.ModCommands
import com.reflectednetwork.rfnetapi.modtools.ModEvents
import com.reflectednetwork.rfnetapi.permissions.PermissionAPI
import com.reflectednetwork.rfnetapi.permissions.PermissionCommands
import com.reflectednetwork.rfnetapi.purchases.PurchaseEvents
import com.reflectednetwork.rfnetapi.purchases.PurchaseGUI
import org.apache.commons.io.IOUtils
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.logging.Level

class RfnetAPI : JavaPlugin(), Listener {
    val ver = description.version.toInt()
    private var disabledForUpdate = false

    private val nextVer = ver + 1
    // Credentials for a read-only user
    private val spaceUser = "31faf87b-0584-449b-b5b4-542b711fedfd"
    private val spacePassword = "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiIzMWZhZjg3Yi0wNTg0LTQ0OWItYjViNC01NDJiNzExZmVkZmQiLCJ" +
            "hdWQiOiIzMWZhZjg3Yi0wNTg0LTQ0OWItYjViNC01NDJiNzExZmVkZmQiLCJvcmdEb21haW4iOiJyZWZsZWN0ZWRuZXR3b3JrIiwibmF" +
            "tZSI6InNlcnZlci11cGRhdGVyIiwiaXNzIjoiaHR0cHM6XC9cL2pldGJyYWlucy5zcGFjZSIsInBlcm1fdG9rZW4iOiIzZE9ueW4zVjY" +
            "4U0oiLCJwcmluY2lwYWxfdHlwZSI6IlNFUlZJQ0UiLCJpYXQiOjE2MjQ0Nzk3MDB9.BmoDM8WFtadwvF9506wpDugq7yNtaiCyWIzfX-" +
            "cC4dGYSQgtTJrISH85fYI0ukD8E4_xFN74xmwa6Pc1gI6kCYICFkk1YIREsdAS08m4KCtAM4iK5YFAD2aodlIOgGVVon7EEqTM8KHBCj" +
            "IyEVk9R-Vj8tLi37j4cnubfo7VkMk"

    var api: ReflectedAPI? = null
    val serverConfig = ServerConfig()
    val database = Database(serverConfig)
    var minigameWorld = false
    private lateinit var permissionAPI: PermissionAPI
    var ghostMode = false

    override fun onEnable() {
        try {
            if (updateCheck()) {
                async {
                    logger.log(Level.SEVERE, "Plugin or dependencies out of date! Will restart server shortly.")
                }
                Bukkit.getScheduler().runTaskLater(this, Runnable { restart() }, 20)
                return
            }

            if (Bukkit.getWorldContainer().list()?.contains("RFNET_GHOST_MODE") == true) {
                ghostMode = true
                GhostModeManager.enable(this)
                return
            }

            // Init the API and let any waiting plugins know that it's ready now.
            api = ReflectedAPI(this)

            // Because of really dumb dependency issues, we've gotta do this
            WorldPluginInterface.plugin = this



            permissionAPI = PermissionAPI(this)
            PermissionCommands.setupCommands()

            ModCommands.initCommands()

            // If something is wrong with the config, shutdown the server, since it won't be connectable.
            if (!serverConfig.isValid()) {
                logger.log(Level.SEVERE, serverConfig.whatsMissing())
                server.shutdown()
            }

            // Setup a plugin messaging channel
            server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

            // Register other API's events
            server.pluginManager.registerEvents(ReflectedAPI.get().commandProvider, this)
            server.pluginManager.registerEvents(ReflectedAPI.get().purchaseAPI, this)

            server.pluginManager.registerEvents(PurchaseGUI, this)
            server.pluginManager.registerEvents(PurchaseEvents, this)
            server.pluginManager.registerEvents(ExceptionDispensary, this)
            server.pluginManager.registerEvents(MedallionAPI, this)
            server.pluginManager.registerEvents(JoinEventWorkaround, this)
            server.pluginManager.registerEvents(PlayerCountEvents, this)
            server.pluginManager.registerEvents(ModEvents, this)
            server.pluginManager.registerEvents(LoginStreakEvents, this)
            server.pluginManager.registerEvents(permissionAPI, this)

            // Check online receipts on occasion.
            server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                try {
                    for (player in server.onlinePlayers) {
                        PurchaseEvents.checkReceipts(player)
                    }
                } catch (e: Exception) {
                    ExceptionDispensary.report(e, "checking receipts")
                }
            }, 400, 400)

            // Also, check for updates if no one is online
            server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                try {
                    if (Bukkit.getOnlinePlayers().isEmpty() && updateCheck()) {
                        restart()
                    }
                } catch (e: Exception) {
                    ExceptionDispensary.report(e, "checking for updates")
                }
            }, 400, 400)

            // Setup default commands, available on every server
            DefaultCommands.initialize()

            // Some stuff should be run AFTER the server has fully loaded.
            server.scheduler.runTaskLater(this, Runnable {
                // Add this server's information to Redis for ServerDiscovery.
                database.setAvailable(true)
            }, 1) // 1 tick, so waits until the server is fully started (started ticking)

            // Stats
            val metrics = Metrics(this, 12072)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "enabling plugin")
        }
    }

    override fun onDisable() {
        try {
            if (ghostMode) {
                GhostModeManager.disable()
                return
            }

            if (!disabledForUpdate) {
                update()
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
            @Suppress("UnstableApiUsage") val out = ByteStreams.newDataOutput()

            // See the spec for this in ServerDiscovery
            out.writeUTF("send:" + player.uniqueId + ":" + archetype)
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray())
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "sending player")
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
            update()

            // And then close the connections to the database
            // so we don't overload them.
            database.close()

            // Wait one second so players don't get Server Closed before being sent back to lobby
            Bukkit.getScheduler().runTaskLater(this, Runnable {
                try {
                    Runtime.getRuntime().addShutdownHook(Thread {
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

    fun updateCheck(): Boolean {
        val worldLoaderJar = File("./plugins/RFNETAPI_WorldLoader-1.jar")
        val protocolLibJar = File("./plugins/ProtocolLib-4.7.0.jar")

        if (!worldLoaderJar.exists() || !protocolLibJar.exists() || (!server.allowFlight || server.onlineMode)) {
            return true
        }

        return try {
            val basicAuthenticationEncoded =
                Base64.getEncoder().encodeToString("$spaceUser:$spacePassword".toByteArray(charset("UTF-8")))
            val url =
                URL("https://maven.pkg.jetbrains.space/reflectednetwork/p/internalapi/maven/com/reflectednetwork/RfnetAPI/$nextVer/RfnetAPI-$nextVer.jar")
            val urlConnection = url.openConnection()
            urlConnection.setRequestProperty("Authorization", "Basic $basicAuthenticationEncoded")
            urlConnection.getInputStream().readAllBytes()
            true
        }  catch (e: FileNotFoundException) {
            false
        } catch (e: IOException) { // We got a 404 meaning the file doesn't exist
            if (e.message?.contains("404") != true) throw e
            false
        }
    }

    private fun update() {
        try {
            println("Checking for updates...")
            // Check for updates
            val pluginUpdateFile = File("./plugins/RfnetAPI-$nextVer.jar")
            try {
                val downloadStream = FileOutputStream(pluginUpdateFile)
                val basicAuthenticationEncoded =
                    Base64.getEncoder().encodeToString("$spaceUser:$spacePassword".toByteArray(charset("UTF-8")))
                val url =
                    URL("https://maven.pkg.jetbrains.space/reflectednetwork/p/internalapi/maven/com/reflectednetwork/RfnetAPI/$nextVer/RfnetAPI-$nextVer.jar")
                val urlConnection = url.openConnection()
                urlConnection.setRequestProperty("Authorization", "Basic $basicAuthenticationEncoded")
                IOUtils.copy(urlConnection.getInputStream(), downloadStream)
                File("./plugins/RfnetAPI-$ver.jar").deleteOnExit()
                println("Update complete!")
            }  catch (e: FileNotFoundException) {
                println("No update found!")
                pluginUpdateFile.delete()
            } catch (e: IOException) { // We got a 404 meaning the file doesn't exist
                if (e.message?.contains("404") != true) throw e
                println("No update found!")
                pluginUpdateFile.delete()
            }

            if (!server.allowFlight || server.onlineMode) {
                println("Applying configuration updates...")

                val properties = Files.lines(Paths.get("server.properties"))
                val newProperties = File("server.properties~").outputStream()

                properties.map {
                    when (it) {
                        "allow-flight=false" -> "allow-flight=true"
                        "network-compression-threshold=256" -> "network-compression-threshold=-1"
                        "view-distance=10" -> "view-distance=5"
                        "allow-nether=true" -> "allow-nether=${serverConfig.archetype == "survival"}"
                        "online-mode=true" -> "online-mode=false"
                        "server-port=25565" -> "server-port=${serverConfig.address.split(":").last()}"
                        else -> it
                    }
                }.forEach { line ->
                    newProperties.write("$line\n".encodeToByteArray())
                }

                Runtime.getRuntime().addShutdownHook(Thread {
                    val propertiesFile = File("server.properties")
                    propertiesFile.delete()
                    Files.move(Paths.get("server.properties~"), Paths.get("server.properties"))
                })

                val themisFolder = File("./plugins/Themis")
                val themisJar = File("./plugins/Themis_0.9.0.jar")
                val protocolLibFolder = File("./plugins/ProtocolLib")
                val protocolLibJar = File("./plugins/ProtocolLib.jar")
                if (themisJar.exists()) {
                    protocolLibJar.deleteOnExit()
                    themisJar.deleteOnExit()
                    Runtime.getRuntime().addShutdownHook(Thread {
                        protocolLibFolder.deleteRecursively()
                        themisFolder.deleteRecursively()
                    })
                }
            }

            println("Checking dependencies...")
            download("https://github.com/dmulloy2/ProtocolLib/releases/download/4.7.0/ProtocolLib.jar", "ProtocolLib-4.7.0")
            download("https://www.dropbox.com/s/od0syes7xubidh3/RFNETAPI_WorldLoader-1.jar?dl=1", "RFNETAPI_WorldLoader-1")
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "updating")
        }
    }

    fun download(urlString: String, pluginName: String) {
        val download = File("./plugins/$pluginName.jar")
        if (!download.exists()) {
            println("Downloading $pluginName.jar")
            val downloadStream = FileOutputStream(download)
            val url = URL(urlString)
            val urlConnection = url.openConnection()
            IOUtils.copy(urlConnection.getInputStream(), downloadStream)
        }
    }
}