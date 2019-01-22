package org.starcoin.sirius.channel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import java.math.BigInteger

class EventBusTest {

    @Test
    fun testEventBus() = runBlocking {
        val eventBus = EventBus<Int>()
        val channel1 = eventBus.subscribe { it % 2 == 0 }
        val channel2 = eventBus.subscribe { it % 2 == 1 }
        launch {
            for (i in 0..9) {
                delay(10)
                //println("send $i")
                eventBus.sendBlocking(i)
            }
            delay(200)
            eventBus.close()
        }
        val list1 = mutableListOf<Int>()
        val list2 = mutableListOf<Int>()
        launch {
            for (i in 0..9) {
                select<Unit> {
                    channel1.onReceiveOrNull { v ->
                        v?.let { list1.add(it) }
                    }
                    channel2.onReceiveOrNull { v ->
                        v?.let { list2.add(it) }
                    }
                }
            }
        }.join()
        Assert.assertEquals(5, list1.size)
        Assert.assertEquals(5, list2.size)
        Assert.assertTrue(list1.all { it % 2 == 0 })
        Assert.assertTrue(list2.all { it % 2 == 1 })
    }

    @Test
    fun testEventBusByHubEvent() = runBlocking {
        val eventBus = EventBus<HubEvent>()
        val address = Address.random()
        val channel1 = eventBus.subscribe { event -> event.isPublicEvent || event.address == address }
        val events = mutableListOf<HubEvent>()
        launch {
            for (e in channel1) {
                events.add(e)
            }
        }
        launch {
            eventBus.sendBlocking(HubEvent(HubEventType.NEW_HUB_ROOT, HubRoot.mock()))
            delay(10)
            for (i in 0..9) {
                val mockTx = OffchainTransaction.mock()
                eventBus.sendBlocking(HubEvent(HubEventType.NEW_TX, OffchainTransaction.mock(), mockTx.to))
                delay(10)
            }
            eventBus.sendBlocking(
                HubEvent(
                    HubEventType.NEW_TX,
                    OffchainTransaction(0, Address.random(), address, BigInteger.ONE),
                    address
                )
            )
            delay(100)
            eventBus.close()
        }.join()
        Assert.assertEquals(2, events.size)
        Assert.assertTrue(events.all { it.isPublicEvent || it.address == address })
        Assert.assertTrue(events.any { it.type == HubEventType.NEW_HUB_ROOT })
    }
}
