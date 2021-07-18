package network.reflected.rfnetapi.purchases

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import network.reflected.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*

object PurchaseGUI : Listener {
    private val title = Component.text("Confirm Dialog").color(NamedTextColor.AQUA)
    private val successCallbacks = hashMapOf<UUID, () -> Unit>()

    fun confirm(player: Player, success: () -> Unit, reason: String) {
        try {
            if (successCallbacks.containsKey(player.uniqueId)) return

            val inv = Bukkit.createInventory(null, InventoryType.HOPPER, title)

            val cancel = ItemStack(Material.RED_WOOL)
            cancel.editMeta {
                it.displayName(Component.text("Cancel Purchase").color(NamedTextColor.RED))
            }
            inv.setItem(1, cancel)

            val lineItem = ItemStack(Material.PAPER)
            lineItem.editMeta {
                it.displayName(Component.text(reason))
            }
            inv.setItem(2, lineItem)

            val confirm = ItemStack(Material.LIME_WOOL)
            confirm.editMeta {
                it.displayName(Component.text("Confirm Purchase").color(NamedTextColor.GREEN))
            }
            inv.setItem(3, confirm)

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
                        successCallbacks[uuid]?.invoke()
                        successCallbacks.remove(uuid)
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