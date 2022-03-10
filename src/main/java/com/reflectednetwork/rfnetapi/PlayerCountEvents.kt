package com.reflectednetwork.rfnetapi

import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerCountEvents : Listener {
    @EventHandler
    private fun playerJoin(event: PlayerJoinEvent) {
        async {
            try {
//                getReflectedAPI().database.updatePlayerCount(Bukkit.getOnlinePlayers().size)
            } catch (e: Exception) {
                ExceptionDispensary.report(e, "player join")
            }
        }
    }

    @EventHandler
    private fun playerQuit(event: PlayerQuitEvent) {
        async {
            try {
//                getReflectedAPI().database.updatePlayerCount(Bukkit.getOnlinePlayers().size)
            } catch (e: Exception) {
                ExceptionDispensary.report(e, "player quit")
            }
        }
    }
}