package org.starcoin.sirius.crypto.fallback;

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoTestBase
import java.math.BigInteger

class DefaultCryptoTest : CryptoTestBase() {
    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is DefaultCryptoKey.Companion)
    }

    @Test
    fun testGeneratePrivateKeyFromBigInteger() {
        val privateKey = DefaultCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE)
        val privateKey1 = DefaultCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE)
        Assert.assertArrayEquals(
            DefaultCryptoKey.encodePrivateKey(privateKey),
            DefaultCryptoKey.encodePrivateKey(privateKey1)
        )
        val data = byteArrayOf(0)
        val sign = DefaultCryptoKey.signData(data, privateKey)

        val publicKey = DefaultCryptoKey.generatePublicKeyFromPrivateKey(privateKey)
        Assert.assertTrue(DefaultCryptoKey.verifySig(data, publicKey, sign))
    }
}
