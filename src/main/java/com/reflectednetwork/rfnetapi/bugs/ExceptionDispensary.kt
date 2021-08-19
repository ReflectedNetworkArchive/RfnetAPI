package com.reflectednetwork.rfnetapi.bugs

import com.mongodb.client.model.Filters.eq
import com.reflectednetwork.rfnetapi.ReflectedAPI
import com.reflectednetwork.rfnetapi.async.async
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.security.SecureRandom

object ExceptionDispensary : Listener {
    private val random = SecureRandom()

    /**
     * Report an exception to the exception store.
     *
     * @param exception The exception to report
     * @param whilst A small string indicating where the exception was caught.
     * @return The ID of the exception
     */
    fun report(exception: Throwable, whilst: String): String {
        val id = miniId()
        val report = StringBuilder()

        report.append("An exception occurred whilst ${whilst.lowercase()}.\n")
        report.append("Exception class: ${exception::class}\n")
        report.append("Exception message: ${exception.message}\n")
        report.append("Internal code in stack:")
        for (line in exception.stackTrace) {
            if (line.toString().contains("reflected")) {
                report.append("\n at $line")
            }
        }

        val exceptions = ReflectedAPI.get().database.getCollection("bugreps", "exceptions")
        val document: String
        val find = exceptions.find(eq("report", report.toString())).first()
        if (find == null) {
            document = id
            exceptions.insertOne(
                Document()
                    .append("report", report.toString())
                    .append("minid", id)
            )
        } else {
            document = find.getString("minid")
        }

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("rfnet.developer")) {
                player.sendMessage(createException(document, report.toString()))
            }
        }

        return document
    }

    fun reportAndNotify(exception: Throwable, whilst: String, user: Audience) {
        user.sendMessage(Component.text("Reporting an exception...").color(NamedTextColor.RED))
        async {
            report(exception, whilst)
        }.then {
            user.sendMessage(
                Component.text(
                    "Auto-report complete. Don't contact an admin.\n" +
                            "Exception ID: $it"
                )
                    .color(NamedTextColor.RED)
            )
        }
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        async {
            if (event.player.hasPermission("rfnet.developer")) {
                ReflectedAPI.get().database.getCollection("bugreps", "exceptions").find().forEach {
                    it?.let {
                        event.player.sendMessage(createException(it.getString("minid"), it.getString("report")))
                    }
                }
            }
        }
    }

    private fun createException(id: String, report: String): Component {
        return Component.text("---- Exception $id ----\n")
            .color(NamedTextColor.RED)
            .append(
                Component.text(report)
                    .color(NamedTextColor.WHITE)
            )
            .append(
                Component.text("\n[ ")
                    .color(NamedTextColor.WHITE)
            ).append(
                Component.text("Erase this Exception")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(
                        ClickEvent.runCommand("/excclear $id")
                    )
            ).append(
                Component.text(" ]")
                    .color(NamedTextColor.WHITE)
            )
    }

    fun miniId(): String {
        val builder = StringBuilder()
        for (i in 1..5) {
            val randInt = random.nextInt(16)
            builder.append(
                when (randInt) {
                    10 -> "a"
                    11 -> "b"
                    12 -> "c"
                    13 -> "d"
                    14 -> "e"
                    15 -> "f"
                    else -> randInt
                }
            )
        }
        return builder.toString()
    }
}