package network.reflected.rfnetapi

import network.reflected.rfnetapi.bugs.ExceptionDispensary
import network.reflected.rfnetapi.commands.CommandRegistry
import network.reflected.rfnetapi.purchases.PurchaseAPI
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.IllegalPluginAccessException

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

    fun getLoadedMap(): World {
        try {
            return plugin.loadedMap?.let {
                Bukkit.getWorld(it.name)
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

/**
 * Returns an instance of the API
 * May be null if called before this plugin loads,
 * so depend on this plugin in your plugin.yml.
 * @see ReflectedAPI.get
 * @return An instance of this class
 */
fun getReflectedAPI(): ReflectedAPI {
    val plugin = Bukkit.getPluginManager().getPlugin("RfnetAPI")
    if (plugin is RfnetAPI && plugin.api != null) {
        return plugin.api!!
    } else {
        throw IllegalPluginAccessException("Plugin must depend on RfnetAPI to use ReflectedAPI.get()")
    }
}