package com.reflectednetwork.rfnetapi

import org.bukkit.Bukkit
import org.bukkit.plugin.IllegalPluginAccessException

/**
 * Returns an instance of the API
 * May be null if called before this plugin loads,
 * so depend on this plugin in your plugin.yml.
 * @see ReflectedAPI.get
 * @return An instance of this class
 */
fun getReflectedAPI(): ReflectedAPI {
    val plugin = Bukkit.getPluginManager().getPlugin("RfnetAPI")
    if (plugin is RfnetAPI && !plugin.ghostMode && plugin.api != null) {
        return plugin.api!!
    } else {
        throw IllegalPluginAccessException("Plugin must depend on RfnetAPI to use getReflectedAPI()")
    }
}