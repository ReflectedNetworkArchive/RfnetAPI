
package network.reflected.rfnetapi.purchases;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player related inventory event
 */
public class PurchaseSuccessEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    protected Product product;
    protected Player purchaser;

    public PurchaseSuccessEvent(@NotNull Product product, @NotNull Player purchaser) {
        this.product = product;
        this.purchaser = purchaser;
    }

    public Product getPurchasedProduct() {
        return this.product;
    }

    public Player getPurchaser() {
        return this.purchaser;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}