package org.starcoin.sirius.crypto.eth

import org.apache.commons.lang3.RandomUtils
import org.ethereum.crypto.ECKey
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoTestBase

class EthCryptoTest : CryptoTestBase() {

    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is EthCryptoService)
    }

    @Test
    fun testEthCrypto() {
        val key = EthCryptoKey()

        val bytes = RandomUtils.nextBytes(32)
        val sign = key.sign(bytes)
        println(sign.size)
        val keyBytes = key.toBytes()

        val key1 = EthCryptoKey(keyBytes)
        val sign1 = key1.sign(bytes)

        Assert.assertEquals(key.address, key1.address)
        Assert.assertEquals(sign, sign1)

        Assert.assertTrue(key.verify(bytes, sign))
        Assert.assertTrue(key.verify(bytes, sign1))
    }

    @Test
    fun testSignatureToECDSASignature(){
        val key = ECKey()
        val bytes = RandomUtils.nextBytes(32)
        val sign = key.sign(bytes)
        val sign1 = sign.toSignature().toECDSASignature()
        Assert.assertEquals(sign, sign1)
    }

}
