@file:Suppress("DEPRECATION")

package com.reflectednetwork.rfnetapi.cclibcompat

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import top.cavecraft.cclib.ICCLib

@Deprecated(message = "ONLY USE for backwards compatibility.")
class ActionBarMessage(val component: Component) : ICCLib.IActionBarMessage {
    override fun sendTo(player: Player) {
        player.sendActionBar(component)
    }
}