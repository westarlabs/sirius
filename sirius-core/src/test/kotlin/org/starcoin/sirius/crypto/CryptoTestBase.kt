package org.starcoin.sirius.crypto

import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Update
import org.starcoin.sirius.util.MockUtils

abstract class CryptoTestBase {

    @Before
    fun setup() {
        val service = CryptoService.instance
        println("CryptoService: ${service.javaClass.name}")
        assertCryptoServiceType(service)
    }

    abstract fun assertCryptoServiceType(service: CryptoService)

    @Test
    fun testGenerateKeyPair() {
        val set = mutableSetOf<CryptoKey>()
        val count = RandomUtils.nextInt(10, 100)
        for (i in 1..count) {
            val keyPair = CryptoService.generateCryptoKey()
            set.add(keyPair)
        }
        //not generate same key
        Assert.assertEquals(count, set.size)
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
        val key = CryptoService.dummyCryptoKey
        val key1 = CryptoService.dummyCryptoKey
        Assert.assertEquals(key, key1)
    }

    @Test
    fun testDummyKeySignature() {
        val hash = Hash.random()
        val key = CryptoService.dummyCryptoKey
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
            Assert.assertTrue(CryptoService.verify(data, sign, key.keyPair.public))
            Assert.assertTrue(CryptoService.verify(data, sign, key))
            Assert.assertTrue(sign.verify(data, key.keyPair.public))
            Assert.assertTrue(sign.verify(data, key))
        }
    }

    @Test
    fun testUpdateSignature() {
        val update = Update.mock()
        val key = CryptoService.generateCryptoKey()
        val hubKey = CryptoService.generateCryptoKey()
        update.sign(key)
        update.signHub(hubKey)
        Assert.assertTrue(update.verfySign(key))
        Assert.assertTrue(update.verifyHubSig(hubKey))
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
