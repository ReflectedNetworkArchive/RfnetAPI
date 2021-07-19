
package network.reflected.rfnetapi.purchases;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player related inventory event
 */
public class AsyncPurchaseSuccessEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    @NotNull
    protected JProduct product;
    @NotNull
    protected Player purchaser;

    public AsyncPurchaseSuccessEvent(@NotNull JProduct product, @NotNull Player purchaser) {
        super(true);
        this.product = product;
        this.purchaser = purchaser;
    }

    @NotNull
    public JProduct getPurchasedProduct() {
        return this.product;
    }

    @NotNull
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