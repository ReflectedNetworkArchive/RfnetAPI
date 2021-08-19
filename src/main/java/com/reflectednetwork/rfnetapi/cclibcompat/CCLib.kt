@file:Suppress("DEPRECATION")

package com.reflectednetwork.rfnetapi.cclibcompat

import com.reflectednetwork.rfnetapi.RfnetAPI
import com.reflectednetwork.rfnetapi.WorldPluginInterface
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
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import top.cavecraft.cclib.ICCLib

object CCLib : ICCLib, JavaPlugin() {
    private lateinit var gameUtils: GameUtils
    lateinit var scoreboardManager: ScoreboardManager

    override fun enableGameFeatures(tickRequirements: MutableList<ICCLib.ITickReq>): ICCLib.IGameUtils {
        gameUtils = GameUtils(tickRequirements)
        return gameUtils
    }

    override fun enableGameFeatures(
        tickRequirements: MutableList<ICCLib.ITickReq>,
        startGame: Runnable,
        teleport: Runnable
    ) {
        gameUtils = GameUtils(tickRequirements, startGame, teleport)
        WorldPluginInterface.plugin?.let { Bukkit.getServer().pluginManager.registerEvents(gameUtils, it) }
    }

    override fun getGameUtils(): ICCLib.IGameUtils {
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

    fun onEnable(plugin: RfnetAPI) {
        scoreboardManager = ScoreboardManager()
        Bukkit.getServer().pluginManager.registerEvents(scoreboardManager, plugin)
    }

    override fun offset(original: Location, xoff: Double, yoff: Double, zoff: Double): Location {
        return original.clone().add(Vector(xoff, yoff, zoff))
    }

    override fun createActionBarMessage(message: String): ICCLib.IActionBarMessage {
        return ActionBarMessage(LegacyComponentSerializer.legacy(ChatColor.COLOR_CHAR).deserialize(message))
    }

    override fun restartServer() {
        getReflectedAPI().restart()
    }

    override fun getScoreboardManager(): ICCLib.IScoreboardManager {
        println("WARNNING: CCLib ScoreBoardManager is enabled. Scoreboards will break unless accessed via legacy functions!")
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