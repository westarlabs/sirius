package org.starcoin.sirius.core


import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction
import org.starcoin.sirius.util.KeyPairUtil

import java.security.PrivateKey
import java.security.PublicKey
import java.util.Objects

@Serializable
class OffchainTransaction : CachedHash, ProtobufCodec<ProtoOffchainTransaction>,
    MerkleTree.MerkleTreeData<ProtoOffchainTransaction>, Mockable {


    var eon: Int = 0
    var from: BlockAddress? = null
    var to: BlockAddress? = null
    var timestamp: Long = 0
    var amount: Long = 0

    @Optional
    var sign: Signature? = null

    constructor() {
        this.timestamp = System.currentTimeMillis()
    }

    constructor(eon: Int, from: BlockAddress, to: BlockAddress, amount: Long) {
        this.eon = eon
        this.from = from
        this.to = to
        this.amount = amount
        this.timestamp = System.currentTimeMillis()
    }

    constructor(transaction: ProtoOffchainTransaction) {
        this.unmarshalProto(transaction)
    }

    constructor(tx: OffchainTransaction) {
        this.eon = tx.eon
        this.from = tx.from
        this.to = tx.to
        this.timestamp = tx.timestamp
        this.amount = tx.amount
        this.sign = tx.sign
    }

    override fun hashData(): ByteArray {
        return this.marshalSignData().build().toByteArray()
    }

    private fun marshalSignData(): ProtoOffchainTransaction.Builder {
        return ProtoOffchainTransaction.newBuilder()
            .setEon(this.eon)
            .setFrom(this.from!!.toByteString())
            .setTo(this.to!!.toByteString())
            .setAmount(this.amount)
            .setTimestamp(this.timestamp)
    }

    override fun marshalProto(): ProtoOffchainTransaction {
        val builder = this.marshalSignData()
        if (this.sign != null) {
            builder.sign = this.sign!!.toByteString()
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: ProtoOffchainTransaction) {
        this.eon = proto.eon
        this.from = BlockAddress.valueOf(proto.from)
        this.to = BlockAddress.valueOf(proto.to)
        this.amount = proto.amount
        this.timestamp = proto.timestamp
        this.sign = if (proto.sign.isEmpty) null else Signature.wrap(proto.sign)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is OffchainTransaction) {
            return false
        }
        val that = o as OffchainTransaction?
        return (eon == that!!.eon
                && timestamp == that.timestamp
                && amount == that.amount
                && from == that.from
                && to == that.to)
    }

    override fun hashCode(): Int {
        return Objects.hash(eon, from, to, timestamp, amount)
    }

    override fun mock(context: MockContext) {
        val keyPair = context.getOrDefault("keyPair", KeyPairUtil.generateKeyPair())
        this.from = BlockAddress.genBlockAddressFromPublicKey(keyPair.public)
        this.to = BlockAddress.random()
        this.amount = RandomUtils.nextLong()
        this.timestamp = System.currentTimeMillis()
    }

    fun sign(privateKey: PrivateKey) {
        this.sign = Signature.of(privateKey, this.marshalSignData().build().toByteArray())
    }

    fun verify(publicKey: PublicKey?): Boolean {
        if (this.sign == null) {
            return false
        }
        if (publicKey == null) {
            return false
        }
        return if (this.from != BlockAddress.genBlockAddressFromPublicKey(publicKey)) {
            false
        } else this.sign!!.verify(publicKey, this.marshalSignData().build().toByteArray())
    }

    companion object {

        init {
            MerkleTree.MerkleTreeData.registerImplement(
                OffchainTransaction::class.java, ProtoOffchainTransaction::class.java
            )
        }

        fun genarateHubTransaction(proto: ProtoOffchainTransaction): OffchainTransaction {
            val transaction = OffchainTransaction()
            transaction.unmarshalProto(proto)
            return transaction
        }
    }
}
