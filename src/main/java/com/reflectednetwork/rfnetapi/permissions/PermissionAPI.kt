package com.reflectednetwork.rfnetapi.permissions

import com.mongodb.client.model.Filters.eq
import com.reflectednetwork.rfnetapi.RfnetAPI
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import com.reflectednetwork.rfnetapi.databasetools.getMutableSet
import com.reflectednetwork.rfnetapi.getReflectedAPI
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permissible
import org.bukkit.permissions.PermissionAttachment
import java.util.*
import kotlin.collections.set

class PermissionAPI(private val plugin: RfnetAPI) : Listener {
    private val permissionAttachmentMap = mutableMapOf<UUID, PermissionAttachment>()

    private val rankColorMap = mapOf(
        Pair("premod", TextColor.color(164, 252, 177)),
        Pair("moderator", TextColor.color(242, 153, 252)),
        Pair("developer", TextColor.color(93, 156, 252)),
        Pair("builder", TextColor.color(252, 180, 156)),
        Pair("plus", TextColor.color(184, 242, 70))
    )

    private val rankNameMap = mapOf(
        Pair("premod", "Mod Trainee"),
        Pair("moderator", "Moderator"),
        Pair("developer", "Developer"),
        Pair("builder", "Build Team"),
        Pair("plus", "+")
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun playerJoinEvent(event: PlayerJoinEvent) {
        try {
            val attachment = event.player.addAttachment(plugin)
            permissionAttachmentMap[event.player.uniqueId] = attachment

            loadPermissions(event.player.uniqueId, attachment)

            val rank = whatRank(event.player)
            event.player.playerListName(Component.text(event.player.name).color(rankColorMap[rank]))
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "loading permissions", event.player)
        }
    }

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        event.renderer(ChatRenderer.viewerUnaware { source, sourceDisplayName, message ->
            val rank = whatRank(source)

            if (rank == "") {
                sourceDisplayName
                    .color(NamedTextColor.DARK_GRAY)
                    .append(
                        Component.text(": ")
                            .color(NamedTextColor.DARK_GRAY)
                    ).append(
                        message.color(NamedTextColor.DARK_GRAY)
                    )
            } else {
                Component.text("[")
                    .color(NamedTextColor.GRAY)
                    .append(
                        Component.text(rankNameMap[rank] ?: "+")
                            .color(rankColorMap[rank])
                    ).append(
                        Component.text("] ")
                            .color(NamedTextColor.GRAY)
                    ).append(
                        sourceDisplayName.color(rankColorMap[rank])
                    ).append(
                        Component.text(": ")
                            .color(NamedTextColor.WHITE)
                    ).append(
                        message.color(NamedTextColor.WHITE)
                    )
            }
        })
    }

    private fun loadPermissions(playerUUID: UUID, permissionAttachment: PermissionAttachment) {
        val groups = getReflectedAPI().database.getCollection("permissions", "groups")
        val players = getReflectedAPI().database.getCollection("permissions", "players")

        players.find(eq("uuid", playerUUID.toString())).first()?.let { playerGroup ->
            groups.find(eq("name", playerGroup.getString("group"))).first()?.let { groupPermissions ->
                permissionAttachment.setPermission("rfnet.rank.${groupPermissions.getString("name")}", true)

                val perms = groupPermissions.getMutableSet<String>("permissions")
                for (permission in perms) {
                    permissionAttachment.setPermission(permission, true)
                }
            }
        }
    }

    private fun whatRank(permissible: Permissible): String {
        var rank = ""
        for (rankname in rankColorMap.keys) {
            if (permissible.hasPermission("rfnet.rank.$rankname")) {
                rank = rankname
            }
        }
        return rank
    }
}