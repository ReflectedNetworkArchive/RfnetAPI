package network.reflected.rfnetapi.commands;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface CommandRunnable {
    public abstract void run(CommandSender executor, CommandArg[] arguments);
}
