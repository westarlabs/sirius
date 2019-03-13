package org.starcoin.sirius.crypto.eth

import org.apache.commons.lang3.RandomUtils
import org.ethereum.core.Transaction
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil
import org.ethereum.util.ByteUtil
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoTestBase
import java.math.BigInteger

class EthCryptoTest : CryptoTestBase() {

    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is EthCryptoService)
    }

    @Test
    fun testKeyPairConvert() {
        val key = EthCryptoKey()
        val key1 = EthCryptoKey(key.keyPair.private)
        Assert.assertEquals(key, key1)
    }

    @Test
    fun testSignatureWithPrivate() {
        val key = EthCryptoKey()
        val hash = Hash.random()
        val sign = key.sign(hash)
        val key1 = EthCryptoKey(key.keyPair.private)
        val sign1 = key1.sign(hash)
        Assert.assertEquals(sign, sign1)
        Assert.assertTrue(key.verify(hash, sign))
        Assert.assertTrue(key1.verify(hash, sign1))
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
    fun testSignatureToECDSASignature() {
        val key = ECKey()
        val bytes = RandomUtils.nextBytes(32)
        val sign = key.sign(bytes)
        val sign1 = sign.toSignature().toECDSASignature()
        Assert.assertEquals(sign, sign1)
    }

    @Test
    fun testSignatureSize() {
        for (i in 0..100) {
            val sign = EthCryptoService.dummyCryptoKey.sign(Hash.random())
            Assert.assertEquals(65, sign.size)
        }
    }

    @Test
    fun testGetAddressFromSignature() {
        val key = EthCryptoKey()
        val bytes = RandomUtils.nextBytes(32)
        val hash = EthCryptoService.hash(bytes)
        val sign = key.sign(hash)
        val addressBytes = ECKey.signatureToAddress(hash.toBytes(), sign.toECDSASignature())
        Assert.assertNotNull(addressBytes)
        Assert.assertEquals(key.address, Address.wrap(addressBytes))
    }

    @Test
    fun testGetAddressFromSignatureWithECKey() {
        val key = ECKey()
        val bytes = RandomUtils.nextBytes(32)
        val hash = HashUtil.sha3(bytes)
        val sign = key.sign(hash)
        val addressBytes = ECKey.signatureToAddress(hash, sign)
        Assert.assertNotNull(addressBytes)
        Assert.assertArrayEquals(key.address, addressBytes)
        Assert.assertEquals(key.pubKeyPoint, ECKey.signatureToKey(hash, sign).pubKeyPoint)
    }

    @Test
    fun testGetAddressFromSignatureWithTransaction() {
        val sender = ECKey()
        val receiver = ECKey()
        val tx = createTx(sender, receiver.address, ByteArray(0), 100)
        Assert.assertArrayEquals(sender.address, tx.sender)
        //TODO wait https://github.com/ethereum/ethereumj/issues/1248
        //Assert.assertNotNull(tx.key)
        //Assert.assertEquals(sender, tx.key)
    }

    private fun createTx(
        sender: ECKey, receiveAddress: ByteArray,
        data: ByteArray, value: Long
    ): Transaction {
        val nonce = BigInteger.ONE
        val tx = Transaction(
            ByteUtil.bigIntegerToBytes(nonce),
            ByteUtil.longToBytesNoLeadZeroes(0),
            ByteUtil.longToBytesNoLeadZeroes(3000000),
            receiveAddress,
            ByteUtil.longToBytesNoLeadZeroes(value),
            data
        )
        tx.sign(sender)
        return tx
    }
}
