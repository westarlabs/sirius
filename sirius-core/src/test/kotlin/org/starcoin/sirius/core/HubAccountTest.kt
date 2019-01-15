package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin

class HubAccountTest : SiriusObjectTestBase<HubAccount>(HubAccount::class) {

    @Test
    fun testHubAccountProtobuf() {
        val hubAccount = HubAccount.mock()
        val bytes = hubAccount.toProtobuf()
        val bytes1 = hubAccount.toProto<Starcoin.HubAccount>().toByteArray()
        Assert.assertArrayEquals(bytes, bytes1)
        val hubAccount1 = HubAccount.parseFromProtobuf(bytes)
        Assert.assertEquals(hubAccount, hubAccount1)
    }
}
