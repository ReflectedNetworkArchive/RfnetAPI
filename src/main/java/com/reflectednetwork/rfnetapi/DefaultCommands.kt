package com.reflectednetwork.rfnetapi

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mongodb.client.model.Filters
import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import com.reflectednetwork.rfnetapi.commands.CommandArguments
import com.reflectednetwork.rfnetapi.medallions.MedallionAPI
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

object DefaultCommands {
    private var availableGames = listOf(
        "spleefrun",
        "survival",
        "squidwars",
        "dev",
        "build"
    )

    fun initialize() {
        getReflectedAPI().commandProvider.registerCommand({ executor: CommandSender, arguments: CommandArguments ->
            if (availableGames.contains(arguments.getString(0))) {
                if (executor is Player) {
                    if (arguments.getString(0) == "build") {
                        if (executor.hasPermission("rfnet.build")) {
                            getReflectedAPI().sendPlayer(executor, "build")
                        } else {
                            executor.sendMessage(Component.text("That server is protected.").color(NamedTextColor.RED))
                        }
                    } else if (arguments.getString(0) == "dev") {
                        if (executor.hasPermission("rfnet.developer")) {
                            getReflectedAPI().sendPlayer(executor, "dev")
                        } else {
                            executor.sendMessage(Component.text("That server is protected.").color(NamedTextColor.RED))
                        }
                    } else if (arguments.getString(0) == "survival") {
                        checkVoteAndSendToSurvival(executor)
                    } else {
                        if (arguments.getString(0) == "spleefrun") {
                            MedallionAPI.giveMedallion(executor, "join_spleefrun", "Play SpleefRun")
                        }
                        getReflectedAPI().sendPlayer(executor, arguments.getString(0))
                    }
                } else {
                    executor.sendMessage(Component.text("Only players can connect to servers.").color(NamedTextColor.RED))
                }
            } else {
                executor.sendMessage(Component.text("That game does not exist.").color(NamedTextColor.RED))
            }
        }, 1, "game")

        getReflectedAPI().commandProvider.registerCommand({ executor: CommandSender, _ ->
            executor.sendMessage(
                Component.text("\n").append(
                    Component.text("Click the link below to join our discord!\n")
                ).append(
                    Component.text("--> ")
                        .color(TextColor.color(36, 198, 166))
                        .append(
                            Component.text("https://discord.gg/avECNchTCf").clickEvent(
                                ClickEvent.openUrl("https://discord.gg/avECNchTCf")
                            ).color(TextColor.color(255, 253, 68))
                        ).append(
                            Component.text(" <--\n").color(TextColor.color(36, 198, 166))
                        )
                )
            )
        }, 0, "discord")

        getReflectedAPI().commandProvider.registerCommands({ executor: CommandSender, _ ->
            if (executor is Player) {
                getReflectedAPI().sendPlayer(executor, "lobby")
            }
        }, 0, "hub", "lobby")

        getReflectedAPI().commandProvider.registerCommand({ executor: CommandSender, _ ->
            executor.sendMessage(
                Component.text("\n")
                    .append(Component.text("Command List"))
                    .append(Component.text(" Click one to run it.").color(NamedTextColor.GRAY))
                    .append(
                        Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                            .append(
                                Component.text("/discord")
                                    .clickEvent(ClickEvent.runCommand("/discord"))
                                    .color(TextColor.color(255, 253, 68))
                                    .append(
                                        Component.text(" - Get help from our discord")
                                            .color(TextColor.color(36, 198, 166))
                                    )
                            )
                    )
                    .append(
                        Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                            .append(
                                Component.text("/balance")
                                    .clickEvent(ClickEvent.runCommand("/balance"))
                                    .color(TextColor.color(255, 253, 68))
                                    .append(
                                        Component.text(" - Check how many shards you have")
                                            .color(TextColor.color(36, 198, 166))
                                    )
                            )
                    )
                    .append(
                        Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                            .append(
                                Component.text("/lobby")
                                    .clickEvent(ClickEvent.runCommand("/lobby"))
                                    .color(TextColor.color(255, 253, 68))
                                    .append(
                                        Component.text(" - Get back to the main lobby")
                                            .color(TextColor.color(36, 198, 166))
                                    )
                            )
                    ).append(Component.text("\n"))
            )
        }, 0, "help")

        getReflectedAPI().commandProvider.registerCommand(
            { _, _ -> getReflectedAPI().restart() },
            "rfnet.restart",
            0,
            "restart"
        )

        getReflectedAPI().commandProvider.registerCommand(
            { _, _ -> getReflectedAPI().restart() },
            "rfnet.restart",
            0,
            "fakerestart"
        )

        getReflectedAPI().commandProvider.registerCommand(
            { executor, arguments ->
                val exceptions = getReflectedAPI().database.getCollection("bugreps", "exceptions")
                if (exceptions.deleteMany(Filters.eq("minid", arguments.getString(0))).deletedCount > 0) {
                    executor.sendMessage(
                        Component.text("☞ ")
                            .color(NamedTextColor.GRAY)
                            .append(
                                Component.text("Item erased!")
                                    .color(NamedTextColor.GREEN)
                            )
                    )
                }
            }, "rfnet.developer", 1, "excclear"
        )

        getReflectedAPI().commandProvider.registerCommands({ executor, _ ->
                async {
                    executor.sendMessage(
                        Component.text("☞ ")
                            .color(NamedTextColor.GRAY)
                            .append(
                                Component.text("Server running Reflected API v${getReflectedAPI().getVersion()}")
                                    .color(NamedTextColor.GREEN)
                            ).append(
                                Component.text(" - Minigame support ${if (getReflectedAPI().isMinigameWorld()) "enabled" else "disabled"}")
                                    .color(NamedTextColor.GRAY)
                            ).append(
                                Component.text(" - Database ${if (getReflectedAPI().database.isConnected()) "connected" else "unavailable"}")
                                    .color(NamedTextColor.GRAY)
                            ).append(
                                Component.text(" - API ${if (WorldPluginInterface.plugin?.updateCheck() == true) "outdated" else "up to date"}")
                                    .color(NamedTextColor.GRAY)
                            )
                    )
                }
            },
            0,
            "version",
            "ver",
            "about"
        )
    }

    private fun checkVoteAndSendToSurvival(player: Player) {
        try {
            if (player.hasPermission("rfnet.rank.plus")) {
                getReflectedAPI().sendPlayer(player, "survival")
                return
            }

            async {
                val url = URL("https://reflected.network/voted/" + player.name)
                val connection = url.openConnection()
                val inputStream = connection.getInputStream()
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val builder = StringBuilder()
                bufferedReader.lines().forEach { str: String? -> builder.append(str) }
                val result = Gson().fromJson(builder.toString(), JsonObject::class.java)
                result["voted"].asBoolean
            }.then {
                if (it) {
                    getReflectedAPI().sendPlayer(player, "survival")
                    MedallionAPI.giveMedallion(player, "join_survivals3", "Join Survival S3")
                } else {
                    player.sendMessage(Component.text("You need to vote before joining survival."))
                    player.openInventory.close()
                    player.openBook(
                        Book.book(
                            Component.text(""),
                            Component.text(""),
                            Component.text("\n\n\nUse the link below vote.\n   ")
                                .color(NamedTextColor.BLACK)
                                .append(
                                    Component.text("Click to open!")
                                        .clickEvent(ClickEvent.openUrl("https://reflected.network/vote"))
                                        .color(NamedTextColor.GREEN)
                                        .decoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE)
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                                )
                        )
                    )
                }
            }
        } catch (e: IOException) {
            ExceptionDispensary.reportAndNotify(e, "checking vote status", player)
        }
    }
}