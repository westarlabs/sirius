package org.starcoin.sirius.hub

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.datastore.MapDataStoreFactory
import kotlin.properties.Delegates

class HubAccountStoreTest {

    var hubAccountStore: HubAccountStore by Delegates.notNull()

    @Before
    fun before() {
        val storeFactory = MapDataStoreFactory()
        hubAccountStore = HubAccountStore(0, storeFactory)
    }

    @Test
    fun testPutGet() {
        val account1 = HubAccount.mock()
        hubAccountStore.put(account1)
        val account2 = hubAccountStore.get(account1.address)!!
        Assert.assertEquals(account1, account2)

        account2.deposit += 1000.toBigInteger()
        hubAccountStore.put(account2)

        val account3 = hubAccountStore.get(account1.address)!!
        Assert.assertEquals(account2, account3)
        Assert.assertNotEquals(account1, account3)
    }

    @After
    fun after() {
        //TODO clean resource
    }
}