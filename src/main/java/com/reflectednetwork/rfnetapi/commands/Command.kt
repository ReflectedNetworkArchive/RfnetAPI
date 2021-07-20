package com.reflectednetwork.rfnetapi.commands

import org.bukkit.command.CommandSender

data class Command(
    val runnable: (executor: CommandSender, arguments: CommandArguments) -> Unit,
    val permission: String?,
    val argCount: Int
)