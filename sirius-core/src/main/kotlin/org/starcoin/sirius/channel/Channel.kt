package org.starcoin.sirius.channel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout


suspend fun <T> Channel<T>.receiveTimeout(timeMillis: Long = 2000): T = withTimeout(timeMillis) {
    receive()
}

