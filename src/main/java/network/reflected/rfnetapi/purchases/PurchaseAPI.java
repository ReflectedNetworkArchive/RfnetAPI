package network.reflected.rfnetapi.purchases;

import org.bukkit.entity.Player;

public class PurchaseAPI { // TODO: Write the methods in this class
    public PurchaseAPI() {
        // Does this even need to be here?
    }

    boolean hasItem(String item, Player player) { // TODO: Implement
        // Check db to see if player owns item
        return false;
    }

    int getShardBalance(Player player) { // TODO: Implement
        return 0;
    }

    boolean purchaseWithShards(String item, Player player) { // TODO: Implement
        // Decrease # of shards, and give the player the item
        return false; // Since this is an empty method it never succeeds.
    }

    boolean addShards(int amount, Player player) { // TODO: Implement
        // Add # of shards to db
        return false; // Since this is an empty method it never succeeds.
    }

    boolean purchaseWithDollars(String item, Player player) { // TODO: Implement
        // Give player the item without reducing shards
        return false; // Since this is an empty method it never succeeds.
    }
}
