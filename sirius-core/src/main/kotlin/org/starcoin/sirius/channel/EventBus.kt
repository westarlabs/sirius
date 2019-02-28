package org.starcoin.sirius.channel

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.sendBlocking

class EventBus<T> {
    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel()

    suspend fun send(event: T) {
        channel.send(event)
    }

    fun sendBlocking(event: T) {
        channel.sendBlocking(event)
    }

    fun subscribe(predicate: (T) -> Boolean = { true }): ReceiveChannel<T> {
        return channel.openSubscription().filter { predicate(it) }
    }

    fun close() {
        this.channel.close()
    }
}
