package network.reflected.rfnetapi.commands;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.*;

public class CommandRegistry implements Listener { // TODO: Needs testing
    private static final CommandRegistry registry = new CommandRegistry();
    Map<String, Command> commands = new HashMap<>();

    public static CommandRegistry getRegistry() {
        return registry;
    }

    public void registerCommand(CommandRunnable command, int numberOfArgs, String name) {
        if (!commands.containsKey(name)) {
            commands.put(name, new Command(command, numberOfArgs));
        } else {
            throw new ArrayStoreException("That command has already been registered!");
        }
    }

    public void registerCommands(CommandRunnable command, int numberOfArgs, String... names ) {
        for (String name : names) {
            registerCommand(command, numberOfArgs, name);
        }
    }

    public boolean executeCommand(CommandSender commandSender, String commandStr) {
        String[] tokenized = tokenize(commandStr);
        if (commands.containsKey(tokenized[0])) return false;
        Command command = commands.get(commandStr);

        if (tokenized.length - 1 != command.argCount) {
            commandSender.sendMessage(Component.text("This command expects " + command.argCount + " arguments."));
            return true;
        }

        command.getRunnable().run(
                commandSender,
                CommandArg.parse(Arrays.copyOfRange(tokenized, 1, tokenized.length - 1))
        );
        return true;
    }

    public static String[] tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        boolean inquotes = false;
        StringBuilder currentToken = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            switch (input.charAt(i)) {
                case ' ':
                    if (inquotes || (i > 0 && input.charAt(i-1) == '\\')) {
                        currentToken.append(' ');
                    } else if (!currentToken.toString().equals("")) {
                        tokens.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    }
                    break;
                case '"':
                    if (i > 0 && input.charAt(i-1) == '\\') {
                        currentToken.append('"');
                    } else {
                        inquotes = true;
                    }
                    break;
                case '\\':
                    if (i > 0 && input.charAt(i-1) == '\\') {
                        currentToken.append('\\');
                    }
                    break;
                default:
                    currentToken.append(input.charAt(i));
                    break;
            }
        }
        if (!currentToken.toString().equals("")) tokens.add(currentToken.toString());
        String[] returnArr = new String[tokens.size()];
        return tokens.toArray(returnArr);
    }

    @EventHandler
    public void playerCommand(AsyncChatEvent event) {
        if (((TextComponent)event.message()).content().startsWith("/")) {
            event.setCancelled(executeCommand(event.getPlayer(), ((TextComponent)event.message()).content().substring(1)));
        }
    }

    @EventHandler
    public void serverCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith("/")) {
            event.setCancelled(executeCommand(event.getSender(), event.getCommand().substring(1)));
        } else {
            event.setCancelled(executeCommand(event.getSender(), event.getCommand()));
        }
    }

    @RequiredArgsConstructor
    public static class Command {
        @Getter final CommandRunnable runnable;
        @Getter final int argCount;
    }
}
