package com.reflectednetwork.rfnetapi

import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ServerConfig {
    private var missingMsg = ""
    private var valid = true
    private val finishedLoad: Boolean
    private val bootConfigFile: File?
    private var bootConfig: YamlConfiguration? = null
    fun isValid(): Boolean {
        // Check and make sure that the server ID field contains only contains ABCabc123
        // (other values may break some redis stuff)
        return valid && finishedLoad
    }

    fun whatsMissing(): String {
        return missingMsg
    }

    val id: String
        get() = bootConfig!!.getString("server-id")!!
    val archetype: String
        get() = bootConfig!!.getString("archetype")!!
    val redisURI: String
        get() = bootConfig!!.getString("redis-lettuce-uri")!!
    val mongoURI: String
        get() = bootConfig!!.getString("mongo-uri")!!
    val connectionStrategy: String
        get() = bootConfig!!.getString("player-connection-strategy")!!
    val isGlobalChatEnabled: Boolean
        get() = bootConfig!!.getBoolean("global-chat")
    val maps: List<String>
        get() = bootConfig!!.getStringList("maps")
    val address: String
        get() = bootConfig!!.getString("connection-string")!!

    // Stolen from bukkit forums if it breaks then idk
    private fun copy(`in`: InputStream, file: File) {
        try {
            val out: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            out.close()
            `in`.close()
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "copying configuration")
        }
    }

    init {
        bootConfigFile = File("./bootconfig.yml")
        if (!bootConfigFile.exists()) {
            bootConfigFile.getParentFile().mkdirs()
            copy(javaClass.classLoader.getResourceAsStream("bootconfig.yml")!!, bootConfigFile)
        }
        if (javaClass.classLoader.getResourceAsStream("bootconfig.yml") == null) {
            Bukkit.getLogger().warning("Template bootconfig.yml in jar is missing. (Something is about to fail)")
        }
        try {
            bootConfig = YamlConfiguration.loadConfiguration(bootConfigFile)
        } catch (e: Exception) {
            missingMsg = "Error loading config file."
            ExceptionDispensary.report(e, "loading configuration")
        }
        if (archetype == "REPLACE-THIS") {
            missingMsg = "You need to fill out bootconfig.yaml in your server's root directory."
            valid = false
        }
        finishedLoad = true
    }
}