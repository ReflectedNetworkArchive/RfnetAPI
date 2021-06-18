package network.reflected.rfnetapi.purchases;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import network.reflected.rfnetapi.commands.CommandRegistry;
import org.bukkit.entity.Player;

public class PurchaseAPI { // TODO: Write the methods in this class
    public PurchaseAPI() {
        CommandRegistry.getRegistry().registerCommand((executor, arguments) -> {
            if (executor instanceof Player) {
                executor.sendMessage(
                        Component.text("You currently have ")
                                .color(TextColor.color(36, 198, 166))
                                .append(Component.text(
                                        getShardBalance((Player) executor) + " shards"
                                ).color(TextColor.color(200, 255, 230)))
                );
            }
        }, 0, "balance");
    }

    public boolean hasItem(String item, Player player) { // TODO: Implement
        // Check db to see if player owns item
        return false;
    }

    public int getShardBalance(Player player) { // TODO: Implement
        return 0;
    }

    public boolean purchaseWithShards(String item, Player player) { // TODO: Implement
        // Decrease # of shards, and give the player the item
        return false; // Since this is an empty method it never succeeds.
    }

    public boolean addShards(int amount, Player player) { // TODO: Implement
        // Add # of shards to db
        return false; // Since this is an empty method it never succeeds.
    }

    public boolean purchaseWithDollars(String item, Player player) { // TODO: Implement
        // Give player the item without reducing shards
        return false; // Since this is an empty method it never succeeds.
    }
}
