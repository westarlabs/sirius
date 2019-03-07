package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test

class AddressTest {

    @Test
    fun testAddressToProto() {
        val address = Address.random()
        val proto = address.toProto()
        val address2 = Address.wrap(proto.address)
        Assert.assertEquals(address, address2)
    }
}