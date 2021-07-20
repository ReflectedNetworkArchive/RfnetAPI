package com.reflectednetwork.rfnetapi.purchases

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

object PurchaseGUI : Listener {
    private val title = Component.text("Confirm Dialog").color(NamedTextColor.AQUA)
    private val successCallbacks = hashMapOf<UUID, () -> Unit>()

    fun confirm(player: Player, success: () -> Unit, reason: String) {
        try {
            successCallbacks.remove(player.uniqueId)

            val inv = Bukkit.createInventory(player, 9, title)

            val cancel = ItemStack(Material.RED_WOOL)
            cancel.editMeta {
                it.displayName(
                    Component.text("Cancel Purchase")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
            inv.setItem(3, cancel)

            val lineItem = ItemStack(Material.PAPER)
            lineItem.editMeta {
                it.displayName(Component.text(reason))
            }
            inv.setItem(4, lineItem)

            val confirm = ItemStack(Material.LIME_WOOL)
            confirm.editMeta {
                it.displayName(
                    Component.text("Confirm Purchase")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
            inv.setItem(5, confirm)

            player.openInventory(inv)

            successCallbacks[player.uniqueId] = success
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "opening confirm dialog", player)
        }
    }

    @EventHandler
    fun inventoryClickEvent(event: InventoryClickEvent) {
        try {
            if (event.view.title() == title) {
                event.isCancelled = true
                when (event.currentItem?.type) {
                    Material.LIME_WOOL -> {
                        val uuid = event.whoClicked.uniqueId
                        val player = Bukkit.getPlayer(uuid)
                        val callback = successCallbacks[uuid]

                        if (player != null && callback != null) {
                            async {
                                callback.invoke()
                            }
                            successCallbacks.remove(uuid)
                        }

                        event.view.close()
                    }
                    Material.RED_WOOL -> {
                        event.view.close()
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "confirm dialog click event")
        }
    }
}