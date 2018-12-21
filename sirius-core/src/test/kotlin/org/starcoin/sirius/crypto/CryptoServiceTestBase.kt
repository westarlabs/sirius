package org.starcoin.sirius.crypto

import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test

abstract class CryptoServiceTestBase {

    @Before
    fun setup() {
        val service = CryptoService.instance
        println("CryptoService: ${service.javaClass.name}")
        assertCryptoServiceType(service)
    }

    abstract fun assertCryptoServiceType(service: CryptoService)

    @Test
    fun testGenerateKeyPair() {
        val keyPair = CryptoService.generateCryptoKey()
        Assert.assertNotNull(keyPair)
        val keyPair1 = CryptoService.generateCryptoKey()
        Assert.assertNotEquals(keyPair, keyPair1)
    }

    @Test
    fun testHash() {
        val data = RandomUtils.nextBytes(RandomUtils.nextInt(100, 1000))
        val hash = CryptoService.hash(data)
        val hash1 = CryptoService.hash(data)
        Assert.assertEquals(hash, hash1)
    }
}
