package org.starcoin.sirius.datastore

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.HubAccount

class SiriusObjectStoreTest {

    @Test
    fun testHashObjectStore() {
        val store = MapStore()
        val objectStore = SiriusObjectStore.hashStore(HubAccount::class, store)
        val hubAccount = HubAccount.mock()
        objectStore.put(hubAccount.id, hubAccount)
        val hubAccount2 = objectStore.get(hubAccount.id)
        Assert.assertEquals(hubAccount, hubAccount2)
    }

    @Test
    fun testAddressObjectStore() {
        val store = MapStore()
        val objectStore = SiriusObjectStore.addressStore(HubAccount::class, store)
        val hubAccount = HubAccount.mock()
        objectStore.put(hubAccount.address, hubAccount)
        val hubAccount2 = objectStore.get(hubAccount.address)
        Assert.assertEquals(hubAccount, hubAccount2)
    }
}