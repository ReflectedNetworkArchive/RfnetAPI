package network.reflected.rfnetapi;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import network.reflected.rfnetapi.commands.CommandArg;
import network.reflected.rfnetapi.commands.CommandRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandRegistry implements Listener { // TODO: Needs testing
    Map<String, Command> commands = new HashMap<>();

    public void registerCommand(CommandRunnable command, String permission, int numberOfArgs, String name) {
        if (!commands.containsKey(name)) {
            if (!Bukkit.getCommandMap().getKnownCommands().containsKey(name)) {
                Bukkit.getCommandMap().register("reflected", new EmptyCommand(name));
            }
            commands.put(name, new Command(command, permission, numberOfArgs));
        } else {
            throw new ArrayStoreException("That command has already been registered!");
        }
    }

    public void registerCommands(CommandRunnable command, String permission, int numberOfArgs, String... names ) {
        for (String name : names) {
            registerCommand(command, permission, numberOfArgs, name);
        }
    }

    public void registerCommand(CommandRunnable command, int numberOfArgs, String name) {
        registerCommand(command, "", numberOfArgs, name);
    }

    public void registerCommands(CommandRunnable command, int numberOfArgs, String... names ) {
        for (String name : names) {
            registerCommand(command, numberOfArgs, name);
        }
    }

    private boolean executeCommand(CommandSender commandSender, String commandStr) {
        String[] tokenized = tokenize(commandStr);
        if (!commands.containsKey(tokenized[0])) {
            return false;
        }
        Command command = commands.get(commandStr);

        if (!command.permission.equals("") && !commandSender.hasPermission(command.permission)) {
            return false; // If they don't have permission, pretend the command doesn't exist.
        }

        if (tokenized.length - 1 != command.argCount) {
            commandSender.sendMessage(Component.text("This command expects " + command.argCount + " arguments."));
            return true;
        }

        command.getRunnable().run(
                commandSender,
                command.argCount == 0 ? null : CommandArg.parse(Arrays.copyOfRange(tokenized, 1, tokenized.length - 1))
        );
        return true;
    }

    private static String[] tokenize(String input) {
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
    private void playerCommand(AsyncChatEvent event) {
        if (((TextComponent)event.message()).content().startsWith("?")) {
            if (!executeCommand(event.getPlayer(), ((TextComponent)event.message()).content().substring(1))) {
                event.getPlayer().sendMessage(Component.text("Command not found."));
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void playerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/")) {
            event.setCancelled(executeCommand(event.getPlayer(), event.getMessage().substring(1)));
        }
    }

    @EventHandler
    private void serverCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith("/")) {
            event.setCancelled(executeCommand(event.getSender(), event.getCommand().substring(1)));
        } else {
            event.setCancelled(executeCommand(event.getSender(), event.getCommand()));
        }
    }

    @RequiredArgsConstructor
    private static class Command {
        @Getter final CommandRunnable runnable;
        @Getter final String permission;
        @Getter final int argCount;
    }

    private static class EmptyCommand extends org.bukkit.command.Command {
        protected EmptyCommand(@NotNull String name) {
            super(name);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return true; // Ignore that this command exists
        }
    }
}
