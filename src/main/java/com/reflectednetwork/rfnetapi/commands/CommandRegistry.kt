package com.reflectednetwork.rfnetapi.commands

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.ServerCommandEvent

class CommandRegistry : Listener {
    var commands: MutableMap<String, Command> = HashMap()
    fun registerCommand(command: (executor: CommandSender, arguments: CommandArguments) -> Unit, permission: String, numberOfArgs: Int, name: String) {
        if (!commands.containsKey(name)) {
            if (!Bukkit.getCommandMap().knownCommands.containsKey(name)) {
                Bukkit.getCommandMap().register("reflected", EmptyCommand(name))
            }
            commands[name] = Command(command, permission, numberOfArgs)
        } else {
            throw ArrayStoreException("That command has already been registered!")
        }
    }

    fun registerCommands(command: (executor: CommandSender, arguments: CommandArguments) -> Unit, permission: String, numberOfArgs: Int, vararg names: String) {
        for (name in names) {
            registerCommand(command, permission, numberOfArgs, name)
        }
    }

    fun registerCommand(command: (executor: CommandSender, arguments: CommandArguments) -> Unit, numberOfArgs: Int, name: String) {
        registerCommand(command, "", numberOfArgs, name)
    }

    fun registerCommands(command: (executor: CommandSender, arguments: CommandArguments) -> Unit, numberOfArgs: Int, vararg names: String) {
        for (name in names) {
            registerCommand(command, numberOfArgs, name)
        }
    }

    private fun executeCommand(commandSender: CommandSender, commandStr: String): Boolean {
        val tokenized = tokenize(commandStr)
        val command = commands[tokenized[0]] ?: return false

        if (command.permission != "" && !commandSender.hasPermission(command.permission!!)) {
            commandSender.sendMessage(Component.text("That command is registered, but it doesn't do anything yet! Please use /help for working commands.").color(NamedTextColor.RED))
            return false // If they don't have permission, pretend the command doesn't exist.
        }

        if (tokenized.size - 1 != command.argCount) {
            commandSender.sendMessage(Component.text("This command expects " + command.argCount + " arguments.").color(NamedTextColor.RED))
            return true
        }

        tokenized.drop(0)
        try {
            command.runnable.invoke(
                commandSender,
                CommandArguments(tokenized)
            )
        } catch (error: ArgumentParseException) {
            commandSender.sendMessage(Component.text(error.errorText).color(NamedTextColor.RED))
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "dispatching command", commandSender)
        }
        return true
    }

    @EventHandler
    private fun playerCommand(event: AsyncChatEvent) {
        if ((event.message() as TextComponent).content().startsWith("?")) {
            if (!executeCommand(event.player, (event.message() as TextComponent).content().substring(1))) {
                event.player.sendMessage(Component.text("Command not found."))
            }
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun playerCommand(event: PlayerCommandPreprocessEvent) {
        if (event.message.startsWith("/")) {
            event.isCancelled = executeCommand(event.player, event.message.substring(1))
        }
    }

    @EventHandler
    private fun serverCommand(event: ServerCommandEvent) {
        if (event.command.startsWith("/")) {
            event.isCancelled = executeCommand(event.sender, event.command.substring(1))
        } else {
            event.isCancelled = executeCommand(event.sender, event.command)
        }
    }

    private class EmptyCommand(name: String) : org.bukkit.command.Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
            return true // Ignore that this command exists
        }
    }

    private fun tokenize(input: String): List<String> {
        val tokens: MutableList<String> = ArrayList()
        var inquotes = false
        var currentToken = StringBuilder()
        for (i in input.indices) {
            when (input[i]) {
                ' ' -> if (inquotes || i > 0 && input[i - 1] == '\\') {
                    currentToken.append(' ')
                } else if (currentToken.toString() != "") {
                    tokens.add(currentToken.toString())
                    currentToken = StringBuilder()
                }
                '"' -> if (i > 0 && input[i - 1] == '\\') {
                    currentToken.append('"')
                } else {
                    inquotes = true
                }
                '\\' -> if (i > 0 && input[i - 1] == '\\') {
                    currentToken.append('\\')
                }
                else -> currentToken.append(input[i])
            }
        }
        if (currentToken.toString() != "") tokens.add(currentToken.toString())
        return tokens
    }
}