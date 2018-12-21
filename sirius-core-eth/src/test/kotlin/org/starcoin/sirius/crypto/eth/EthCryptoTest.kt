package org.starcoin.sirius.crypto.eth

import org.apache.commons.lang3.RandomUtils
import org.ethereum.crypto.ECKey
import org.junit.Assert
import org.junit.Ignore
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

        Assert.assertTrue(key.verify(bytes, sign))
        Assert.assertTrue(key.verify(bytes, sign1))
    }

    @Ignore
    @Test
    fun testECSignature(){
        val key = ECKey()
        val bytes = RandomUtils.nextBytes(32)
        val sign = key.sign(bytes)
        val signBytes = sign.toByteArray()
        val sign1 = ECKey.ECDSASignature.decodeFromDER(signBytes)
        Assert.assertEquals(sign, sign1)
    }

}
