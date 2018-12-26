package org.starcoin.sirius.core

import com.google.protobuf.InvalidProtocolBufferException
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.SiriusObject.Companion.parseFromProtobuf
import org.starcoin.sirius.crypto.CryptoService

class OffchainTransactionTest {

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testOffchainTransaction() {
        val from = Address.random()
        val to = Address.random()

        val tx = OffchainTransaction(0, from, to, RandomUtils.nextLong())

        val hash = tx.hash()

        val bytes = tx.toProtobuf()
        val tx1 = OffchainTransaction.parseFromProtobuf(bytes)
        Assert.assertEquals(tx, tx1)

        Assert.assertEquals(hash, tx1.hash())
    }

    @Test
    fun testOffchainTransactionSignatureAndHash() {
        val from = Address.random()
        val to = Address.random()

        val tx = OffchainTransaction(0, from, to, RandomUtils.nextLong())

        val hash = tx.hash()

        val bytes = tx.toProtobuf()

        val tx1: OffchainTransaction = parseFromProtobuf(bytes)

        Assert.assertEquals(tx, tx1)
        Assert.assertEquals(hash, tx1.hash())

        val json = tx.toJSON()
        println(json)
        val tx2 = OffchainTransaction.parseFromJSON(json)
        Assert.assertEquals(tx, tx2)
        Assert.assertEquals(hash, tx2.hash())

        //add sign
        tx.sign(CryptoService.getDummyCryptoKey())

        val bytes1 = tx.toProtobuf()

        val tx3: OffchainTransaction = parseFromProtobuf(bytes1)
        Assert.assertEquals(tx, tx3)
        Assert.assertEquals(hash, tx3.hash())

    }

    @Test
    fun testTxSign() {
        val key = CryptoService.getDummyCryptoKey()
        val tx = OffchainTransaction.mock()
        tx.sign(key)
        Assert.assertTrue(tx.verify(key))
        val tx1 = OffchainTransaction.parseFromProtobuf(tx.toProtobuf())
        Assert.assertEquals(tx, tx1)
        Assert.assertTrue(tx1.verify(key))
    }
}
