package com.reflectednetwork.rfnetapi.bugs

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.Listener
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
        exception.printStackTrace()
        return ""
    }

    fun reportAndNotify(exception: Throwable, whilst: String, user: Audience) {
        exception.printStackTrace()
        user.sendMessage(Component.text("Internal error occured."))
    }

//    @EventHandler
//    fun playerJoinEvent(event: PlayerJoinEvent) {
//        async {
//            if (event.player.hasPermission("rfnet.developer")) {
//                ReflectedAPI.get().database.getCollection("bugreps", "exceptions").find().forEach {
//                    it?.let {
//                        event.player.sendMessage(createException(it.getString("minid"), it.getString("report")))
//                    }
//                }
//            }
//        }
//    }

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

    fun report(exception: Exception, whilst: String): String = report(exception as Throwable, whilst) // compat
}