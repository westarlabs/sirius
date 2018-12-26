package org.starcoin.sirius.core

import com.google.protobuf.InvalidProtocolBufferException
import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.util.KeyPairUtil
import org.starcoin.sirius.util.MockUtils

class ChainTransactionTest {

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testChainTransaction() {
        val from = Address.random()
        val to = Address.random()

        val arguments = Starcoin.DepositRequest.newBuilder().setAddress(from.toByteString()).setAmount(100).build()
        val tx = ChainTransaction(from, to, System.currentTimeMillis(), 0, "test", arguments)
        val ping = Starcoin.ProtoMsgPing.newBuilder().setNonce(MockUtils.nextInt().toLong()).build()
        tx.receipt = Receipt(true, ping)
        Assert.assertNotNull(tx.arguments)
        Assert.assertEquals(arguments, tx.getArguments(Starcoin.DepositRequest::class.java))

        val hash = tx.hash()

        val bytes = tx.toProto().toByteArray()
        val tx1 = ChainTransaction(Starcoin.ProtoChainTransaction.parseFrom(bytes))
        Assert.assertEquals(tx, tx1)

        Assert.assertEquals(hash, tx1.hash())
    }

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testTxSign() {
        val keyPair = KeyPairUtil.generateKeyPair()
        val tx = ChainTransaction()
        tx.mock(MockContext().put("keyPair", keyPair))
        tx.sign(keyPair)
        Assert.assertTrue(tx.verify())
        val tx1 = ChainTransaction(Starcoin.ProtoChainTransaction.parseFrom(tx.marshalProto().toByteString()))
        Assert.assertEquals(tx, tx1)
        Assert.assertTrue(tx1.verify())
    }

    @Test
    fun testContractTx() {
        val p0kp = KeyPairUtil.generateKeyPair()
        val p0 = Participant(p0kp.public)
        val amount = MockUtils.nextLong()
        val tx = ChainTransaction(p0.address!!, Constants.CONTRACT_ADDRESS, amount)
        tx.sign(p0kp)
        Assert.assertTrue(tx.verify())
    }
}
