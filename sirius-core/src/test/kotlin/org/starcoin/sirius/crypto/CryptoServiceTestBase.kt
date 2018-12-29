package org.starcoin.sirius.crypto

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.util.MockUtils

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
        val data = MockUtils.nextBytes(MockUtils.nextInt(100, 1000))
        val hash = CryptoService.hash(data)
        val hash1 = CryptoService.hash(data)
        Assert.assertEquals(hash, hash1)
    }

    @Test
    fun testDummyKey() {
        val key = CryptoService.getDummyCryptoKey()
        val key1 = CryptoService.getDummyCryptoKey()
        Assert.assertEquals(key, key1)
    }

    @Test
    fun testDummyKeySignature() {
        val hash = Hash.random()
        val key = CryptoService.getDummyCryptoKey()
        val sign = key.sign(hash)
        Assert.assertTrue(key.verify(hash, sign))
    }

    @Test
    fun testLoadKey() {
        val key = CryptoService.generateCryptoKey()
        val publicKeyBytes = CryptoService.encodePublicKey(key.keyPair.public)
        val privateKeyBytes = CryptoService.encodePrivateKey(key.keyPair.private)
        val publicKey = CryptoService.loadPublicKey(publicKeyBytes)
        val privateKey = CryptoService.loadPrivateKey(privateKeyBytes)
        Assert.assertArrayEquals(publicKeyBytes, CryptoService.encodePublicKey(publicKey))
        Assert.assertArrayEquals(privateKeyBytes, CryptoService.encodePrivateKey(privateKey))
    }

    @Test
    fun testSignature() {
        val key = CryptoService.generateCryptoKey()
        for (i in 0..9) {
            val data = MockUtils.nextBytes(Hash.LENGTH)
            val sign = key.sign(data)
            Assert.assertTrue(key.verify(data, sign))
        }
    }

    @Test
    fun testEncodeAndDecode() {
        val key = CryptoService.generateCryptoKey()
        val bytes = key.toBytes()
        val key1 = CryptoService.loadCryptoKey(bytes)
        Assert.assertEquals(key, key1)
    }

    @Test
    fun testAddress() {
        val key = CryptoService.generateCryptoKey()
        val address = key.address
        val address1 = CryptoService.generateAddress(key.keyPair.public)
        Assert.assertEquals(address, address1)
    }

}
