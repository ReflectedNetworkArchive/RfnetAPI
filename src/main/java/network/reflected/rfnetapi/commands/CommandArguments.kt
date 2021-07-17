package network.reflected.rfnetapi.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class CommandArguments(private val stringArguments: List<String>) {
    fun getString(index: Int): String {
        return stringArguments[index]
    }

    fun getPlayer(index: Int): Player {
        val argument = stringArguments[index]
        return try {
            Bukkit.getPlayer(UUID.fromString(argument))
                ?: throw ArgumentParseException("Argument ${index + 1} must be a player.")
        } catch (error: IllegalArgumentException) {
            Bukkit.getPlayer(argument) ?: throw ArgumentParseException("Argument ${index + 1} must be a player.")
        }
    }

    fun getInt(index: Int): Int {
        val argument = stringArguments[index]
        return if (argument.contains(".")) {
            throw ArgumentParseException("Argument ${index + 1} must be an integer.")
        } else {
            try {
                argument.toInt()
            } catch (error: NumberFormatException) {
                throw ArgumentParseException("Argument ${index + 1} must be an integer.")
            }
        }
    }

    fun getFloat(index: Int): Float {
        return try {
            stringArguments[index].toFloat()
        } catch (error: NumberFormatException) {
            throw ArgumentParseException("Argument ${index + 1} must be an integer.")
        }
    }

    fun getDouble(index: Int): Double {
        return try {
            stringArguments[index].toDouble()
        } catch (error: NumberFormatException) {
            throw ArgumentParseException("Argument ${index + 1} must be an integer.")
        }
    }

    fun getBoolean(index: Int): Boolean {
        return when (stringArguments[index].lowercase()) {
            "true" -> true
            "false" -> false
            "yes" -> true
            "no" -> false
            "y" -> true
            "n" -> false
            "t" -> true
            "f" -> false
            else -> throw ArgumentParseException("Argument ${index + 1} must be a yes or no.")
        }
    }
}