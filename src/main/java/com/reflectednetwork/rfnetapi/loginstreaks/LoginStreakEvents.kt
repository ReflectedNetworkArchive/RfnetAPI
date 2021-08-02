package com.reflectednetwork.rfnetapi.loginstreaks

import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Instant

object LoginStreakEvents : Listener {
    const val secondsInDay = 86400

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        val redis = getReflectedAPI().database.getRedis()

        val key = "${event.player.uniqueId}-lastjoinday"
        val today = (Instant.now().epochSecond + 300) / secondsInDay
        val lastJoinDay = redis.get(key)?.toLong()

        if (lastJoinDay != today) {
            val streak = redis.incr("${event.player.uniqueId}-joinstreak").toLong()
            if (streak >= 5L && streak % 5L == 0L && event.player.hasPermission("rfnet.rank.plus")) {
                event.player.sendMessage(Component.text("You won 3 gems for a $streak day login streak! Login for 5 more days to get more!"))
                getReflectedAPI().purchaseAPI.addShards(event.player, 3)
            } else if (streak > 1L) {
                event.player.sendMessage(Component.text("Login streak: $streak days."))
            }
        } else {
            redis.set("${event.player.uniqueId}-joinstreak", "1")
        }

        redis.set(key, today.toString())
    }
}