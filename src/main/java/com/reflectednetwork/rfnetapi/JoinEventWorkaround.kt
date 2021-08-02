package com.reflectednetwork.rfnetapi

import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object JoinEventWorkaround : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    private fun playerJoin(event: PlayerJoinEvent) {
        async {
            try {
                tryMapTP(event.player)
            } catch (e: Exception) {
                ExceptionDispensary.report(e, "player world teleport")
            }
        }
    }

    private fun tryMapTP(player: Player) {
        if (getReflectedAPI().isMinigameWorld()) {
            val location = player.location.clone()
            location.world = getReflectedAPI().getLoadedMap()
            player.teleport(location)
        }
    }
}