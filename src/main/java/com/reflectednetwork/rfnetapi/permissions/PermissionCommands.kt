package com.reflectednetwork.rfnetapi.permissions

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import com.reflectednetwork.rfnetapi.databasetools.getMutableSet
import com.reflectednetwork.rfnetapi.databasetools.updateOneOrCreate
import com.reflectednetwork.rfnetapi.getReflectedAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bson.Document

object PermissionCommands {
    fun setupCommands() {
        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")

            if (groups.countDocuments(eq("name", arguments.getString(0))) > 0) {
                executor.sendMessage(Component.text("That group already exists!").color(NamedTextColor.RED))
            } else {
                groups.insertOne(
                    Document()
                        .append("name", arguments.getString(0))
                        .append("permissions", mutableSetOf<String>())
                )
                executor.sendMessage(Component.text("Group created.").color(NamedTextColor.GREEN))
            }
        }, "rfnet.permissions.edit", 1, "creategroup")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")

            if (groups.countDocuments(eq("name", arguments.getString(0))) <= 0) {
                executor.sendMessage(Component.text("That group doesn't exist!").color(NamedTextColor.RED))
            } else {
                groups.findOneAndDelete(
                    eq("name", arguments.getString(0))
                )
                executor.sendMessage(Component.text("Group deleted.").color(NamedTextColor.GREEN))
            }
        }, "rfnet.permissions.edit", 1, "deletegroup")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")

            val group = groups.find(eq("name", arguments.getString(1))).first()
            if (group == null) {
                executor.sendMessage(Component.text("That group doesn't exist!").color(NamedTextColor.RED))
            } else {
                if (!executor.hasPermission("rfnet.permissions.edit") && arguments.getPlayer(0).hasPermission("rfnet.permissions.moderate")) {
                    executor.sendMessage(Component.text("You can't edit other moderators!").color(NamedTextColor.RED))
                } else {
                    if (group.getMutableSet<String>("permissions")
                            .contains("rfnet.permissions.edit") && !executor.hasPermission("rfnet.permissions.edit")
                    ) {
                        executor.sendMessage(Component.text("You can't escalate privileges!").color(NamedTextColor.RED))
                    } else {
                        val players = getReflectedAPI().database.getCollection("permissions", "players")

                        players.updateOneOrCreate(
                            eq("uuid", arguments.getPlayer(0).uniqueId.toString()),
                            set("group", arguments.getString(1))
                        ) {
                            Document()
                                .append("uuid", arguments.getPlayer(0).uniqueId.toString())
                                .append("group", arguments.getString(1))
                        }

                        executor.sendMessage(Component.text("Group set.").color(NamedTextColor.GREEN))
                    }
                }
            }
        }, "rfnet.permissions.moderate", 2, "setgroup")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val players = getReflectedAPI().database.getCollection("permissions", "players")

            if (!executor.hasPermission("rfnet.permissions.edit") && arguments.getPlayer(0).hasPermission("rfnet.permissions.moderate")) {
                executor.sendMessage(Component.text("You can't edit other moderators!").color(NamedTextColor.RED))
            } else {
                if (players.findOneAndDelete(eq("uuid", arguments.getPlayer(0).uniqueId.toString())) != null) {
                    executor.sendMessage(Component.text("Removed group from player.").color(NamedTextColor.GREEN))
                } else {
                    executor.sendMessage(Component.text("Player didn't have a group set.").color(NamedTextColor.RED))
                }
            }
        }, "rfnet.permissions.moderate", 1, "unsetgroup")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")
            val filter = eq("name", arguments.getString(0))
            val document = groups.find(filter).first()
            if (document == null) {
                executor.sendMessage(Component.text("That group doesn't exist!").color(NamedTextColor.RED))
            } else {
                val perms = document.getMutableSet<String>("permissions")
                perms.add(arguments.getString(1))
                groups.updateOne(filter, set("permissions", perms))
                executor.sendMessage(Component.text("Permission added.").color(NamedTextColor.GREEN))
            }
        }, "rfnet.permissions.edit", 2, "addperm")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")
            val filter = eq("name", arguments.getString(0))
            val document = groups.find(filter).first()
            if (document == null) {
                executor.sendMessage(Component.text("That group doesn't exist!").color(NamedTextColor.RED))
            } else {
                val perms = document.getMutableSet<String>("permissions")
                perms.remove(arguments.getString(1))
                groups.updateOne(filter, set("permissions", perms))
                executor.sendMessage(Component.text("Permission removed.").color(NamedTextColor.GREEN))
            }
        }, "rfnet.permissions.edit", 2, "removeperm")

        getReflectedAPI().commandProvider.registerCommand({ executor, arguments ->
            val groups = getReflectedAPI().database.getCollection("permissions", "groups")
            val filter = eq("name", arguments.getString(0))
            val document = groups.find(filter).first()
            if (document == null) {
                executor.sendMessage(Component.text("That group doesn't exist!").color(NamedTextColor.RED))
            } else {
                val perms = document.getMutableSet<String>("permissions")
                executor.sendMessage(Component.text("Permissions:").color(NamedTextColor.GREEN))
                for (permission in perms) {
                    executor.sendMessage(
                        Component.text(" - ")
                            .color(NamedTextColor.GRAY)
                            .append(
                                Component.text(permission)
                                    .color(NamedTextColor.WHITE)
                            )
                    )
                }
            }
        }, "rfnet.permissions.edit", 1, "listperms")
    }
}