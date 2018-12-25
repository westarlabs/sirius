package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test

class HubAccountTest : SiriusObjectTestBase<HubAccount>(HubAccount::class) {

    @Test
    fun testHubAccountProtobuf() {
        val hubAccount = HubAccount.mock()
        val bytes = hubAccount.toProtobuf()
        val hubAccount1 = HubAccount.parseFromProtobuf(bytes)
        Assert.assertEquals(hubAccount, hubAccount1)
    }
}
