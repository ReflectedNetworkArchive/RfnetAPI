package network.reflected.rfnetapi.commands;

import org.bukkit.command.CommandSender;

public interface CommandRunnable {
    void run(CommandSender executor, CommandArg[] arguments);
}
