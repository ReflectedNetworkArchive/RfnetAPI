package com.reflectednetwork.rfnetapi

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object GhostModeManager : Listener {
    lateinit var plugin: RfnetAPI

    fun enable(plugin: RfnetAPI) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
        plugin.database.setAvailable(true)
    }

    fun disable() {
        plugin.database.setAvailable(false)
        plugin.database.close()
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        event.player.isOp = true
        event.player.sendMessage(
            Component.text("""Server is in ghost mode.
                | The API is disabled.
                | Permissions are disabled.
                | You are an operator.
                | Database connectivity is restricted.
                | To go to the lobby, chat "lobby"
            """.trimMargin()).color(NamedTextColor.RED)
        )
        event.player.addAttachment(plugin).setPermission("gopaint.use", true)
    }

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        val message = event.message()
        if (message is TextComponent && message.content() == "lobby") {
            event.player.kick(Component.text("Server closed"))
        }
    }
}