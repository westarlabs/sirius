package org.starcoin.sirius.crypto.fallback;

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoServiceTestBase
import java.math.BigInteger

class FallbackCryptoServiceTest : CryptoServiceTestBase() {
    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is FallbackCryptoKey.Companion)
    }

    @Test
    fun testGeneratePrivateKeyFromBigInteger() {
        val privateKey = FallbackCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE)
        val privateKey1 = FallbackCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE)
        Assert.assertArrayEquals(
            FallbackCryptoKey.encodePrivateKey(privateKey),
            FallbackCryptoKey.encodePrivateKey(privateKey1)
        )
        val data = byteArrayOf(0)
        val sign = FallbackCryptoKey.signData(data, privateKey)

        val publicKey = FallbackCryptoKey.generatePublicKeyFromPrivateKey(privateKey)
        Assert.assertTrue(FallbackCryptoKey.verifySig(data, publicKey, sign))
    }
}
