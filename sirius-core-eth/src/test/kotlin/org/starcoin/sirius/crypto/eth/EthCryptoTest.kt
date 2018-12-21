package org.starcoin.sirius.crypto.eth

import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test

class EthCryptoTest {

    @Test
    fun testEthCrypto() {
        val key = EthCryptoKey()

        val bytes = RandomUtils.nextBytes(32)
        val sign = key.sign(bytes)

        val keyBytes = key.toBytes()

        val key1 = EthCryptoKey(keyBytes)
        val sign1 = key1.sign(bytes)

        Assert.assertEquals(key.getAddress(), key1.getAddress())
        Assert.assertEquals(sign, sign1)
    }
}
