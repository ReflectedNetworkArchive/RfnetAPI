@file:Suppress("DEPRECATION")

package com.reflectednetwork.rfnetapi.cclibcompat

import top.cavecraft.cclib.ICCLib.ITickReq

@Deprecated(message = "For compatibility with older plugins")
class TickReq(//Players necessary to shorten to the number of ticks
    val numberOfPlayers: Int, seconds: Int, gameCanStart: Boolean
) : ITickReq {
    val numberOfTicks: Int
    val ifGameCanStart: Boolean

    init {
        numberOfTicks = seconds * 20
        ifGameCanStart = gameCanStart
    }
}