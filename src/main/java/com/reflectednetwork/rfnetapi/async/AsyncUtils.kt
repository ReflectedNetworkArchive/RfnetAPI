package com.reflectednetwork.rfnetapi.async

import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bukkit.Bukkit

fun <T> async(function: () -> T) : AsyncReturnable<T> {
    val returnable = AsyncReturnable<T>()
    Bukkit.getScheduler().runTaskLaterAsynchronously(
        Bukkit.getPluginManager().getPlugin("RfnetAPI") ?: Bukkit.getPluginManager().plugins[0],
        Runnable {
            try {
                val result = function.invoke()
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("RfnetAPI") ?: Bukkit.getPluginManager().plugins[0],
                    Runnable {
                        try {
                            returnable.setReturn(result)
                        } catch (e: Exception) {
                            ExceptionDispensary.report(e, "running < async.then >")
                        }
                    }
                )
            } catch (e: Exception) {
                ExceptionDispensary.report(e, "running < async function >")
            }
        },
        1
    )
    return returnable
}