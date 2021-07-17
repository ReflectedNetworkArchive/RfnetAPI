package network.reflected.rfnetapi

import network.reflected.rfnetapi.commands.CommandRegistry
import network.reflected.rfnetapi.purchases.PurchaseAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

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

    companion object {
        /**
         * Returns an instance of the API
         * May be null if called before this plugin loads,
         * so depend on this plugin in your plugin.yml.
         * @see ReflectedAPI.get
         * @return An instance of this class
         */
        fun get(): ReflectedAPI {
            val plugin = Bukkit.getPluginManager().getPlugin("RfnetAPI")
            if (plugin is RfnetAPI) {
                return plugin.api!!
            } else {
                throw NullPointerException("Plugin must depend on RfnetAPI")
            }
        }
    }
}