@file:Suppress("DEPRECATION")

package com.reflectednetwork.rfnetapi.cclibcompat

import com.reflectednetwork.rfnetapi.WorldPluginInterface.plugin
import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import top.cavecraft.cclib.ICCLib.IGameUtils
import top.cavecraft.cclib.ICCLib.IGameUtils.GemAmount
import top.cavecraft.cclib.ICCLib.ITickReq
import java.util.*

/**
 * This class exists only for backwards compatibility.
 */
@Deprecated("This class exists only for backwards compatibility")
class GameUtils : IGameUtils, Listener {
    private val returnToLobbyName: Component = Component.text("Return to Lobby").color(NamedTextColor.RED)
    private var ticksToStart = 0
    private var ticksPassed = 0
    private var maxPlayers = 0
    private val teleportToArenaTicks = 60
    private var tickRequirements: List<ITickReq>
    private var startGame: Runnable
    private var teleport: Runnable
    private var started = false
    private val interactBlockList = listOf(
        Material.ACACIA_DOOR,
        Material.BIRCH_DOOR,
        Material.DARK_OAK_DOOR,
        Material.IRON_DOOR,
        Material.JUNGLE_DOOR,
        Material.SPRUCE_DOOR,
        Material.OAK_TRAPDOOR,
        Material.OAK_DOOR,
        Material.IRON_TRAPDOOR,
        Material.LEVER,
        Material.STONE_BUTTON,
        Material.OAK_BUTTON
    )

    constructor(tickRequirements: List<ITickReq>) {
        this.tickRequirements = tickRequirements
        startGame = Runnable {}
        teleport = Runnable {}
        for (tickReq in tickRequirements) {
            if ((tickReq as TickReq).numberOfTicks > ticksToStart) {
                ticksToStart = tickReq.numberOfTicks
            }
            if (tickReq.numberOfPlayers > maxPlayers) {
                maxPlayers = tickReq.numberOfPlayers
            }
        }
        Bukkit.getServer().scheduler.runTaskTimer(plugin!!, Runnable {}, 0, 1)
    }

    constructor(tickRequirements: List<ITickReq>, startGame: Runnable, teleport: Runnable) {
        this.tickRequirements = tickRequirements
        this.startGame = startGame
        this.teleport = teleport
        for (tickReq in tickRequirements) {
            if ((tickReq as TickReq).numberOfTicks > ticksToStart) {
                ticksToStart = tickReq.numberOfTicks
            }
            if (tickReq.numberOfPlayers > maxPlayers) {
                maxPlayers = tickReq.numberOfPlayers
            }
        }
        Bukkit.getServer().scheduler.runTaskTimer(plugin!!, Runnable { tick() }, 0, 1)
    }

    private fun tick() {
        if (!started) {
            preStart()
        }
    }

    private fun preStart() {
        val players = Bukkit.getServer().onlinePlayers
        if (players.size > 1) {
            for (tickReq in tickRequirements) {
                if (players.size == (tickReq as TickReq).numberOfPlayers && ticksPassed < ticksToStart - tickReq.numberOfTicks) {
                    ticksPassed = ticksToStart - tickReq.numberOfTicks
                }
                if (ticksPassed == ticksToStart - tickReq.numberOfTicks) {
                    val message = ChatColor.GOLD.toString() + "Game starts regardless of player count in"
                    val time = " " + ChatColor.BLUE + (ticksToStart - ticksPassed) / 20 + "s"
                    Bukkit.getServer().broadcastMessage(message + time)
                    for (player in players) {
                        player.playSound(player.location, Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f)
                    }
                }
            }
            if (ticksPassed == ticksToStart - teleportToArenaTicks) {
                teleport.run()
                getReflectedAPI().setAvailable(false)
                clearGameBox(getReflectedAPI().getLoadedMap())
                Bukkit.getServer().scheduler.runTaskLater(plugin!!, Runnable {
                    Bukkit.getServer().broadcastMessage(ChatColor.GREEN.toString() + "Go!")
                    for (player in players) {
                        player.resetTitle()
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)
                    }
                    startGame.run()
                    started = true
                }, teleportToArenaTicks.toLong())
            }
            if (ticksPassed >= ticksToStart - teleportToArenaTicks && ticksPassed % 20 == 0 && ticksToStart - ticksPassed != 0) {
                for (player in players) {
                    player.sendTitle("" + ChatColor.RED + (ticksToStart - ticksPassed) / 20, "")
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.75f)
                }
            }
            ticksPassed++
        } else if (ticksPassed != 0) {
            ticksPassed = 0
        }
    }

    @EventHandler
    fun leave(event: PlayerQuitEvent?) {
        if (Bukkit.getServer().onlinePlayers.size <= 1 && started) {
            getReflectedAPI().restart()
        }
    }

    @EventHandler
    fun hunger(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

    @EventHandler
    fun playerInteract(event: PlayerInteractEvent) {
        if (!started) {
            kickIfLobbyTP(event.item!!, event.player)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        event.joinMessage =
            ChatColor.GOLD.toString() + event.player.name + " joined. " + ChatColor.BLUE + "(" + Bukkit.getServer().onlinePlayers.size + "/" + maxPlayers + ")"
        setupFreshPlayer(event.player)
    }

    override fun blockInteract(event: PlayerInteractEvent) {
        try {
            if (event.clickedBlock == null) return
            if (interactBlockList.contains(event.clickedBlock!!.type)) {
                event.isCancelled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    override fun awardGems(player: Player, amount: GemAmount) {}
    override fun setupFreshPlayer(player: Player) {
        try {
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            player.teleport(Location(getReflectedAPI().getLoadedMap(), 0.0, 100.0, 0.0))
            player.inventory.setItem(
                8,
                createNamedItem(Material.TNT, 1, returnToLobbyName)
            ) //Slot 8 is the far right hotbar slot
            player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, true))
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    @Deprecated("")
    private fun createNamedItem(mat: Material, count: Int, name: Component): ItemStack {
        val item = ItemStack(mat, count)
        item.editMeta { it: ItemMeta -> it.displayName(name.decoration(TextDecoration.ITALIC, false)) }
        return item
    }

    override fun kickIfLobbyTP(item: ItemStack, player: Player) {
        try {
            if (Objects.requireNonNull(item.itemMeta.displayName())!!
                    .contains(returnToLobbyName)
            ) {
                getReflectedAPI().sendPlayer(player, "lobby")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    override fun winnerEffect(winner: Player) {
        try {
            winner.gameMode = GameMode.CREATIVE
            winner.isFlying = true
            Bukkit.getServer().scheduler.runTaskTimer(plugin!!, Runnable {
                val firework = winner.world.spawnEntity(winner.location, EntityType.FIREWORK) as Firework
                val fireworkMeta = firework.fireworkMeta
                fireworkMeta.power = 1
                if (Math.random() > 0.5) {
                    fireworkMeta.addEffect(
                        FireworkEffect.builder().with(FireworkEffect.Type.STAR).flicker(true).withColor(
                            Color.ORANGE
                        ).build()
                    )
                } else {
                    fireworkMeta.addEffect(
                        FireworkEffect.builder().with(FireworkEffect.Type.CREEPER).flicker(true).withColor(
                            Color.GREEN
                        ).build()
                    )
                }
                firework.fireworkMeta = fireworkMeta
            }, 0, 10)
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    override fun deathEffect(player: Player) {
        try {
            if (player.gameMode == GameMode.SURVIVAL) {
                player.teleport(Location(player.world, 0.0, 65.0, 0.0))
                player.gameMode = GameMode.SPECTATOR
                Bukkit.getServer()
                    .broadcastMessage(ChatColor.GOLD.toString() + player.displayName + ChatColor.WHITE + " died. " + ChatColor.GOLD + "FINAL KILL")
                player.world.playSound(player.location, Sound.ENTITY_BAT_DEATH, 8f, 1f)
                player.hidePlayer(player)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    override fun deathMessage(player: Player) {
        try {
            if (player.gameMode == GameMode.SURVIVAL) {
                Bukkit.getServer()
                    .broadcastMessage(ChatColor.GOLD.toString() + player.displayName + ChatColor.WHITE + " died.")
                player.sendTitle(ChatColor.RED.toString() + "You died.", "")
                Bukkit.getServer().scheduler.runTaskLater(plugin!!, Runnable { player.resetTitle() }, (20 * 2).toLong())
                player.teleport(Location(player.world, 0.0, 65.0, 0.0))
                player.gameMode = GameMode.SPECTATOR
                player.world.playSound(player.location, Sound.ENTITY_BAT_DEATH, 8f, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

    override fun clearGameBox(world: World) {
        try {
            for (x in -15..14) {
                for (y in 122 downTo 97) {
                    for (z in -15..14) {
                        val block = world.getBlockAt(x, y, z)
                        block.type = Material.AIR
                    }
                }
            }
            for (item in world.getEntitiesByClass(Item::class.java)) {
                if (item != null && item.location.y >= 96) {
                    item.remove()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }
}