package com.reflectednetwork.rfnetapi.modtools

import com.mongodb.client.model.Filters.*
import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import kotlin.math.floor

object ModEvents : Listener {
    @EventHandler
    fun asyncPlayerPreLoginEvent(event: AsyncPlayerPreLoginEvent) {
        val bans = getReflectedAPI().database.getCollection("punishments", "bans")

        val ban = bans.find(
            and(
                eq("uuid", event.playerProfile.id.toString()),
                gt("banExpires", System.currentTimeMillis()))
        ).first()

        if (ban != null) {
            val banid = ban.getObjectId("_id").toString()
            val reason = ban.getString("reason")
            val timeLeftMS = ban.getLong("banExpires") - System.currentTimeMillis()
            val timeLeft = if (timeLeftMS <= oneDayMS) {
                DurationFormatUtils.formatDuration(
                    timeLeftMS,
                    "HH:mm:ss"
                )
            } else {
                "${floor((timeLeftMS / oneDayMS).toDouble())} days"
            }

            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                Component.text("You are banned.\n\n")
                    .color(NamedTextColor.GREEN)
                    .append(
                        Component.text("Ban ID: $banid\n")
                            .color(NamedTextColor.YELLOW)
                    ).append(
                        Component.text("$reason\n\n")
                            .color(NamedTextColor.YELLOW)
                    ).append(
                        Component.text("$timeLeft left.")
                            .color(NamedTextColor.AQUA)
                    )
            )
        }
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        if (event.player.hasPermission("rfnet.ban")) {
            val bans = getReflectedAPI().database.getCollection("punishments", "bans")
            val savedbans = getReflectedAPI().database.getCollection("punishments", "savedbans")

            savedbans.find().forEach {
                it?.let { document ->
                    event.player.sendMessage(
                        Component.text("Ban for review:")
                            .color(NamedTextColor.GREEN)
                            .append(
                                Component.text("\nPlayer: ")
                                    .color(NamedTextColor.GRAY)
                            ).append(
                                Component.text(document.getString("playername"))
                                    .color(NamedTextColor.YELLOW)
                            ).append(
                                Component.text("\nReason: ")
                                    .color(NamedTextColor.GRAY)
                            ).append(
                                Component.text(document.getString("reason"))
                                    .color(NamedTextColor.YELLOW)
                            ).append(
                                Component.text("\nMarked for ban by: ")
                                    .color(NamedTextColor.GRAY)
                            ).append(
                                Component.text(document.getString("author"))
                                    .color(NamedTextColor.YELLOW)
                            ).append(
                                Component.text("\nBAN PLAYER")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(
                                        ClickEvent.runCommand("/bs3 ${document.getString("uuid")} \"${document.getString("reason")}\"")
                                    )
                            ).append(
                                Component.text(" CANCEL BAN")
                                    .color(NamedTextColor.RED)
                                    .clickEvent(
                                        ClickEvent.runCommand("/bunsave ${document.getString("uuid")}")
                                    )
                            )
                    )
                }
            }
        }
    }
}