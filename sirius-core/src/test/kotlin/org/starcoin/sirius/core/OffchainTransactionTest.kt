package org.starcoin.sirius.core

import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction
import org.starcoin.sirius.util.KeyPairUtil

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

    @ImplicitReflectionSerializer
    @Test
    fun testOffchainTransactionSerializable() {
        try {
            val from = Address.random()
            val to = Address.random()

            val tx = OffchainTransaction(0, from, to, RandomUtils.nextLong())

            val hash = tx.hash()

            val bytes = ProtoBuf.dump(tx)

            val tx1: OffchainTransaction = ProtoBuf.load(bytes)

            Assert.assertEquals(tx, tx1)
            Assert.assertEquals(hash, tx1.hash())

            val json = JSON.stringify(tx)
            println(json)
            val tx2 = JSON.parse<OffchainTransaction>(json)
            Assert.assertEquals(tx, tx2)
            Assert.assertEquals(hash, tx2.hash())

            //add sign
            tx.sign(KeyPairUtil.TEST_KEYPAIR.private)

            val bytes1 = ProtoBuf.dump(tx)

            val tx3: OffchainTransaction = ProtoBuf.load(bytes1)
            Assert.assertEquals(tx, tx3)
            Assert.assertEquals(hash, tx3.hash())
            Assert.fail()
        } catch (ex: SerializationException) {
            //TODO wait new version Serialization
        }

    }

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testTxSign() {
        val keyPair = KeyPairUtil.generateKeyPair()
        val tx = OffchainTransaction.mock()
        //tx.mock(MockContext().put("keyPair", keyPair))
        tx.sign(keyPair.private)
        Assert.assertTrue(tx.verify(keyPair.public))
        val tx1 = OffchainTransaction.parseFromProtoMessage(
            ProtoOffchainTransaction.parseFrom(tx.toProtobuf())
        )
        Assert.assertEquals(tx, tx1)
        Assert.assertTrue(tx1.verify(keyPair.public))
    }
}
