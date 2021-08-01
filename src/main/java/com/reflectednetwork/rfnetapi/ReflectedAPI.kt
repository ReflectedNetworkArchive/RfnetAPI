package com.reflectednetwork.rfnetapi

import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import com.reflectednetwork.rfnetapi.commands.CommandRegistry
import com.reflectednetwork.rfnetapi.purchases.PurchaseAPI
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

class ReflectedAPI internal constructor(private val plugin: RfnetAPI) {
    val purchaseAPI = PurchaseAPI()
    val commandProvider = CommandRegistry()

    fun setAvailable(available: Boolean) {
        try {
            plugin.database.setAvailable(available)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "setting server availability")
        }
    }

    fun getVersion(): Int {
        return plugin.ver
    }

    fun restart() {
        try {
            plugin.restart()
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "restarting")
        }
    }

    val database: Database
        get() = plugin.database

    fun sendPlayer(player: Player, archetype: String) {
        try {
            plugin.sendPlayer(player, archetype)
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "sending player")
        }
    }

    fun isMinigameWorld(): Boolean {
        return plugin.minigameWorld
    }

    fun getLoadedMap(): World {
        try {
            return WorldPluginInterface.worldName?.let {
                Bukkit.getWorld(it)
            } ?: Bukkit.getWorlds()[0]
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "getting loaded map")
            throw NullPointerException("Error whilst getting loaded map. This was reported to the internal exception log.")
        }
    }

    companion object { // Back compat
        fun get(): ReflectedAPI {
            return getReflectedAPI()
        }
    }
}