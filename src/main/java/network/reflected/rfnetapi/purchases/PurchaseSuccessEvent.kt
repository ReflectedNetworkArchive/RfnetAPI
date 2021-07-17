package network.reflected.rfnetapi.purchases

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Represents a player related inventory event
 */
class PurchaseSuccessEvent(var purchasedProduct: Product, var purchaser: Player) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        val handlerList = HandlerList()
    }
}