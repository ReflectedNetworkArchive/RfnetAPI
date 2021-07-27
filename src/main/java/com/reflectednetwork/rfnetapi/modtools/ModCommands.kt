package com.reflectednetwork.rfnetapi.modtools

import com.mongodb.client.model.Filters.eq
import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bson.Document
import org.bukkit.Bukkit
import java.util.*

object ModCommands {
    val reasons = listOf(
        "Rule 1: Discrimination or Slurs",
        "Rule 2: EULA Violation or Criminal acts",
        "Rule 3: Harmful or Explicit Content",
        "Rule 4: Alternate Account",
        "Rule 5: Cheating, Exploits, or Modified Client"
    )

    val banTimes = listOf(
        sevenDaysMS,
        thirtyDaysMS,
        Long.MAX_VALUE
    )

    fun initCommands() {
        getReflectedAPI().commandProvider.registerCommand({executor, _ ->
            executor.sendMessage(Component.text("Select a player:").color(NamedTextColor.GREEN))

            for (player in Bukkit.getOnlinePlayers()) {
                executor.sendMessage(
                    Component.text(" - ")
                        .color(NamedTextColor.GRAY)
                        .append(
                            Component.text(player.name)
                                .color(NamedTextColor.WHITE)
                                .clickEvent(
                                    ClickEvent.runCommand("/bs2 ${player.uniqueId}")
                                )
                        )
                )
            }
        }, "rfnet.startban", 0, "ban")

        getReflectedAPI().commandProvider.registerCommand({executor, arguments ->
            executor.sendMessage(Component.text("Select a reason:").color(NamedTextColor.GREEN))

            val command = if (executor.hasPermission("rfnet.ban")) {
                "bs3"
            } else {
                "bsave"
            }

            for (reason in reasons) {
                executor.sendMessage(
                    Component.text(" - ")
                        .color(NamedTextColor.GRAY)
                        .append(
                            Component.text(reason)
                                .color(NamedTextColor.WHITE)
                                .clickEvent(
                                    ClickEvent.runCommand("/$command ${arguments.getPlayer(0).uniqueId} \"$reason\"")
                                )
                        )
                )
            }

            executor.sendMessage(
                Component.text(" - ")
                    .color(NamedTextColor.GRAY)
                    .append(
                        Component.text("Custom reason")
                            .color(NamedTextColor.WHITE)
                            .clickEvent(
                                ClickEvent.suggestCommand("/$command ${arguments.getPlayer(0).uniqueId} \"Custom reason\"")
                            )
                    )
            )
        }, "rfnet.startban", 1, "bs2")

        getReflectedAPI().commandProvider.registerCommand({executor, arguments ->
            val bans = getReflectedAPI().database.getCollection("punishments", "bans")

            bans.insertOne(
                Document()
                    .append("uuid", arguments.getString(0))
                    .append("banExpires", System.currentTimeMillis() + banTimes[
                            bans.countDocuments(eq("uuid", arguments.getString(0))).toInt()
                    ])
                    .append("reason", arguments.getString(1))
            )

            val player = Bukkit.getPlayer(UUID.fromString(arguments.getString(0)))
            player?.kick(Component.text("You have been banned. Rejoin for punishment info.").color(NamedTextColor.RED))

            val savedbans = getReflectedAPI().database.getCollection("punishments", "savedbans")
            savedbans.findOneAndDelete(eq("uuid", arguments.getString(0)))

            executor.sendMessage(Component.text("Player banned!").color(NamedTextColor.GREEN))
        }, "rfnet.ban", 2, "bs3")

        getReflectedAPI().commandProvider.registerCommand({executor, arguments ->
            val savedbans = getReflectedAPI().database.getCollection("punishments", "savedbans")

            savedbans.insertOne(
                Document()
                    .append("uuid", arguments.getString(0))
                    .append("reason", arguments.getString(1))
                    .append("author", executor.name)
                    .append("playername", Bukkit.getOfflinePlayer(UUID.fromString(arguments.getString(0))).name)
            )

            executor.sendMessage(
                Component.text("Your ban was saved, a moderator will approve it soon.")
                    .color(NamedTextColor.GREEN)
            )
        }, "rfnet.startban", 2, "bsave")

        getReflectedAPI().commandProvider.registerCommand({executor, arguments ->
            val savedbans = getReflectedAPI().database.getCollection("punishments", "savedbans")
            savedbans.findOneAndDelete(eq("uuid", arguments.getString(0)))
            executor.sendMessage(
                Component.text("☞ ")
                    .color(NamedTextColor.GRAY)
                    .append(
                        Component.text("Ban cancelled!")
                            .color(NamedTextColor.GREEN)
                    )
            )
        }, "rfnet.ban", 1, "bunsave")
    }
}