package network.reflected.rfnetapi.medallions

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import network.reflected.rfnetapi.async.async
import network.reflected.rfnetapi.bugs.ExceptionDispensary
import network.reflected.rfnetapi.databasetools.findOneOrCreate
import network.reflected.rfnetapi.databasetools.getCollection
import network.reflected.rfnetapi.databasetools.updateOneOrCreate
import org.bson.Document
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object MedallionAPI : Listener {
    fun increaseStat(player: Player, statistic: String, friendlyName: String) {
        async {
            val statsCollection = getCollection("medallions", "statistics")

            statsCollection.updateOneOrCreate(
                eq("uuid", player.uniqueId.toString()),
                min(statistic, 0)
            ) {
                Document()
                    .append("uuid", player.uniqueId.toString())
                    .append(statistic, 1)
            }

            statsCollection.updateOne(
                eq("uuid", player.uniqueId.toString()),
                inc(statistic, 1)
            )

            async {
                statsCollection.find(eq("uuid", player.uniqueId.toString())).first()?.getInteger(statistic) ?: 0
            }.then {
                when (it) {
                    1 -> giveMedallion(player, "${statistic}_1", "1st $friendlyName")
                    10 -> giveMedallion(player, "${statistic}_10", "10th $friendlyName")
                    50 -> giveMedallion(player, "${statistic}_50", "50th $friendlyName")
                    100 -> giveMedallion(player, "${statistic}_100", "100th $friendlyName")
                }
            }
        }
    }

    fun giveMedallion(player: Player, achievement: String, friendlyName: String) {
        async {
            try {
                val medallionCollection = getCollection("medallions", "medallions")
                val medallionsDocument = medallionCollection.findOneOrCreate(
                    eq("uuid", player.uniqueId.toString())
                ) {
                    Document()
                        .append("uuid", player.uniqueId.toString())
                        .append("achievements", mutableListOf<String>())
                        .append("achievementsFriendly", mutableListOf<String>())
                }

                if (medallionsDocument?.getList("achievements", String::class.java)?.contains(achievement) != true) {
                    var medallions = medallionsDocument?.getList("achievements", String::class.java)
                    if (medallions == null) {
                        medallions = mutableListOf<String>()
                    }
                    medallions.add(achievement)
                    var niceNameMedallions = medallionsDocument?.getList("achievementsFriendly", String::class.java)
                    if (niceNameMedallions == null) {
                        niceNameMedallions = mutableListOf<String>()
                    }
                    niceNameMedallions.add(friendlyName)
                    medallionCollection.updateOne(
                        eq("uuid", player.uniqueId.toString()),
                        combine(set("achievements", medallions), set("achievementsFriendly", niceNameMedallions))
                    )
                    return@async true
                }
                false
            } catch (e: Exception) {
                ExceptionDispensary.report(e, "giving medallion")
                false
            }
        }.then {
            if (it) {
                player.sendMessage(
                    Component.text("☞ ")
                        .color(NamedTextColor.GOLD)
                        .append(
                            Component.text("You got a medallion: ")
                                .color(TextColor.color(149, 249, 164))
                        ).append(
                            Component.text(friendlyName)
                                .color(TextColor.color(222, 252, 241))
                        ).append(
                            Component.text(" ☜")
                                .color(NamedTextColor.GOLD)
                        )
                )
            }
        }
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        giveMedallion(event.player, "firstjoin", "1st Join")
    }
}