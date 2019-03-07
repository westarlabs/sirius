package org.starcoin.sirius.channel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.sendBlocking

class EventBus<T> {

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel()

    @UseExperimental(ExperimentalCoroutinesApi::class)
    suspend fun send(event: T) {
        channel.send(event)
    }

    fun sendBlocking(event: T) {
        channel.sendBlocking(event)
    }

    @UseExperimental(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    fun subscribe(predicate: (T) -> Boolean = { true }): ReceiveChannel<T> {
        return channel.openSubscription().filter { predicate(it) }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun close() {
        this.channel.close()
    }
}
