@file:Suppress("DEPRECATION")

package com.reflectednetwork.rfnetapi.cclibcompat

import com.reflectednetwork.rfnetapi.WorldPluginInterface.plugin
import com.reflectednetwork.rfnetapi.cclibcompat.scoreboard.ChildScoreboard
import com.reflectednetwork.rfnetapi.cclibcompat.scoreboard.ScoreboardManager
import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import top.cavecraft.cclib.ICCLib

object CCLib : ICCLib {
    private lateinit var gameUtils: GameUtils
    lateinit var scoreboardManager: ScoreboardManager

    override fun enableGameFeatures(tickRequirements: MutableList<ICCLib.ITickReq>): ICCLib.IGameUtils {
        if (!this::scoreboardManager.isInitialized) {
            scoreboardManager = ScoreboardManager()
            plugin?.let { Bukkit.getServer().pluginManager.registerEvents(scoreboardManager, it) }
        }
        gameUtils = GameUtils(tickRequirements)
        return gameUtils
    }

    override fun enableGameFeatures(
        tickRequirements: MutableList<ICCLib.ITickReq>,
        startGame: Runnable,
        teleport: Runnable
    ) {
        if (!this::scoreboardManager.isInitialized) {
            println("WARNNING: CCLib ScoreBoardManager is enabled. Scoreboards will break unless accessed via legacy functions!")
            scoreboardManager = ScoreboardManager()
            plugin?.let { Bukkit.getServer().pluginManager.registerEvents(scoreboardManager, it) }
        }
        gameUtils = GameUtils(tickRequirements, startGame, teleport)
        plugin?.let { Bukkit.getServer().pluginManager.registerEvents(gameUtils, it) }
    }

    override fun getGameUtils(): ICCLib.IGameUtils {
        if (!this::scoreboardManager.isInitialized) {
            println("WARNNING: CCLib ScoreBoardManager is enabled. Scoreboards will break unless accessed via legacy functions!")
            scoreboardManager = ScoreboardManager()
            plugin?.let { Bukkit.getServer().pluginManager.registerEvents(scoreboardManager, it) }
        }
        return gameUtils
    }

    override fun createTickReq(numberOfPlayers: Int, seconds: Int, canStart: Boolean): ICCLib.ITickReq {
        return TickReq(numberOfPlayers, seconds, canStart)
    }

    override fun createNamedItem(material: Material, amount: Int, name: String): ItemStack {
        val item = ItemStack(material, amount)
        item.editMeta {
            it.displayName(LegacyComponentSerializer.legacy(ChatColor.COLOR_CHAR).deserialize(name).decoration(TextDecoration.ITALIC, false))
        }
        return item
    }

    override fun offset(original: Location, xoff: Double, yoff: Double, zoff: Double): Location {
        return original.clone().add(Vector(xoff, yoff, zoff))
    }

    override fun createActionBarMessage(message: String): ICCLib.IActionBarMessage {
        return ActionBarMessage(LegacyComponentSerializer.legacy(ChatColor.COLOR_CHAR).deserialize(message))
    }

    override fun restartServer() {
        if (!(this::gameUtils.isInitialized && gameUtils.awardedwinner)) {
            getReflectedAPI().restart()
        }
    }

    override fun getScoreboardManager(): ICCLib.IScoreboardManager {
        if (!this::scoreboardManager.isInitialized) {
            println("WARNNING: CCLib ScoreBoardManager is enabled. Scoreboards will break unless accessed via legacy functions!")
            scoreboardManager = ScoreboardManager()
            plugin?.let { Bukkit.getServer().pluginManager.registerEvents(scoreboardManager, it) }
        }
        scoreboardManager.enableCompat()
        return scoreboardManager
    }

    override fun awardGems(player: Player?, amount: ICCLib.IGameUtils.GemAmount?) { }

    override fun getGems(player: Player?): Double { return 0.0 }

    override fun decreaseGems(player: Player?, decrease: Double) { }

    override fun createChildScoreboard(): ICCLib.IChildScoreboard {
        return ChildScoreboard()
    }
}