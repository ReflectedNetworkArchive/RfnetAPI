package network.reflected.rfnetapi.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.FloodgateAPI;

import java.util.Objects;
import java.util.UUID;

public class PlayerArg extends CommandArg {
    UUID player = null;

    public PlayerArg(String originalArgument) {
        super(originalArgument);
        if (Bukkit.getPlayer(UUID.fromString(originalArgument)) != null) {
            player = UUID.fromString(originalArgument);
        } else if (Bukkit.getPlayer(originalArgument) != null) {
            player = Bukkit.getPlayer(originalArgument).getUniqueId();
        } else if (Bukkit.getPlayer("." + originalArgument) != null && FloodgateAPI.isBedrockPlayer(Objects.requireNonNull(Bukkit.getPlayer("." + originalArgument)))) {
            player = Bukkit.getPlayer("." + originalArgument).getUniqueId();
        }
    }

    public static boolean is(String playerArg) {
        return new PlayerArg(playerArg).isValid();
    }

    public boolean isValid() {
        return player != null;
    }

    public Player get() {
        return Bukkit.getPlayer(player);
    }
}
