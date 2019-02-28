package org.starcoin.sirius.channel

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeout


suspend fun <T> ReceiveChannel<T>.receiveTimeout(timeMillis: Long = 2000): T = withTimeout(timeMillis) {
    receive()
}

