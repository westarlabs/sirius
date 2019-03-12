package org.starcoin.sirius.hub

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.datastore.MapDataStoreFactory
import java.math.BigInteger
import kotlin.random.Random

class EonStateTest {

    @Test
    fun testEonStateEonSwitch() {
        val eonState0 = EonState(0)
        val hubAccount0 = HubAccount.mock()
        eonState0.saveAccount(hubAccount0)
        Assert.assertEquals(hubAccount0, eonState0.getAccount(hubAccount0.address))
        val originDeposit = hubAccount0.deposit

        hubAccount0.addDeposit(BigInteger.TEN)
        eonState0.saveAccount(hubAccount0)

        Assert.assertEquals(BigInteger.TEN + originDeposit, eonState0.getAccount(hubAccount0.address).deposit)

        val randomAccountCount = Random.nextInt(10, 100)
        for (i in 1..randomAccountCount) {
            eonState0.saveAccount(HubAccount.mock())
        }

        val eonState1 = eonState0.toNextEon()

        Assert.assertEquals(hubAccount0.balance, eonState1.getAccount(hubAccount0.address).allotment)

        val hubAccount1 = HubAccount.mock()
        hubAccount1.addDeposit(BigInteger.TEN)
        eonState1.saveAccount(hubAccount1)

        val eonState2 = eonState1.toNextEon()
        var accountCount = 0
        eonState2.forEach { accountCount += 1 }

        Assert.assertEquals(randomAccountCount + 2, accountCount)
    }

    @Test
    fun testEonStateGetProof() {
        val dataFactory = MapDataStoreFactory()
        val eonState0 = EonState(0, dataFactory)
        val hubAccount0 = HubAccount.mock()
        eonState0.saveAccount(hubAccount0)
        val eonState1 = eonState0.toNextEon()
        val proof1 = eonState1.state.getMembershipProof(hubAccount0.address)
        Assert.assertNotNull(proof1)

        val eonStateInitByStore = EonState(1, dataFactory)
        val proof2 = eonStateInitByStore.state.getMembershipProof(hubAccount0.address)
        Assert.assertNotNull(proof2)
        Assert.assertEquals(proof1, proof2)

        Assert.assertNotNull(eonStateInitByStore.previous)
        Assert.assertEquals(eonState0.eon, eonStateInitByStore.previous!!.eon)
    }
}