package network.reflected.rfnetapi

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
        plugin.database.setAvailable(available)
    }

    fun restart() {
        plugin.restart()
    }

    val database: Database
        get() = plugin.database

    fun sendPlayer(player: Player?, archetype: String?) {
        plugin.sendPlayer(player!!, archetype!!)
    }

    fun getLoadedMap(): World {
        return if (plugin.minigameWorld) {
            Bukkit.getWorlds()[0]
        } else {
            Bukkit.getWorld(plugin.loadedMap.name) ?: Bukkit.getWorlds()[0]
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