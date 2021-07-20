package network.reflected.rfnetapi.purchases

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import network.reflected.rfnetapi.ReflectedAPI
import network.reflected.rfnetapi.async.async
import network.reflected.rfnetapi.bugs.ExceptionDispensary
import network.reflected.rfnetapi.medallions.MedallionAPI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PurchaseEvents : Listener {
    @EventHandler
    fun purchaseSuccessEvent(event: AsyncPurchaseSuccessEvent) {
        event.purchaser.sendMessage(
            Component.text("☞ ")
                .color(NamedTextColor.GRAY)
                .append(
                    Component.text("Purchase completed!\n")
                        .color(NamedTextColor.GREEN)
                ).append(
                    Component.text("See? Easy peasy! You now own the ${event.product.niceName}")
                        .color(TextColor.color(200, 200, 200))
                        .decoration(TextDecoration.ITALIC, true)
                )
        )

        MedallionAPI.increaseStat(event.purchaser, "purchase", "In-Game Purchase")
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        checkReceipts(event.player)
    }

    fun checkReceipts(player: Player) {
        try {
            async {
                val purchases = ReflectedAPI.get().database.getCollection("purchases", "purchases")

                purchases.find(
                    and(
                        eq("uuid", player.uniqueId.toString()),
                        eq("needsToSee", true)
                    )
                ).first()?.let {
                    purchases.updateOne(
                        eq("uuid", player.uniqueId.toString()),
                        set("needsToSee", false)
                    )
                    return@async true
                }
                false
            }.then { needsToSee ->
                if (needsToSee) {
                    async {
                        ReflectedAPI.get().purchaseAPI.getShardBalance(player)
                    } .then {
                        player.sendMessage(
                            Component.text("☞ ")
                                .color(NamedTextColor.GRAY)
                                .append(
                                    Component.text("New balance: $it shards.")
                                        .color(NamedTextColor.GREEN)
                                )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "checking receipts")
        }
    }
}