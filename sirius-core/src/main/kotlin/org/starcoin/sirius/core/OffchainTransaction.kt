package org.starcoin.sirius.core


import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import java.security.PrivateKey
import java.security.PublicKey

//TODO ensure timestamp
@Serializable
@ProtobufSchema(Starcoin.OffchainTransactionData::class)
data class OffchainTransactionData(
    @SerialId(1)
    val eon: Int = 0,
    @SerialId(2)
    val from: Address,
    @SerialId(3)
    val to: Address,
    @SerialId(4)
    val amount: Long = 0,
    @SerialId(5)
    val timestamp: Long = System.currentTimeMillis()
) : SiriusObject() {

    companion object :
        SiriusObjectCompanion<OffchainTransactionData, Starcoin.OffchainTransactionData>(OffchainTransactionData::class) {
        override fun mock(): OffchainTransactionData {
            return super.mock()
        }

        override fun parseFromProtoMessage(proto: Starcoin.OffchainTransactionData): OffchainTransactionData {
            return OffchainTransactionData(
                proto.eon,
                Address.wrap(proto.from),
                Address.wrap(proto.to),
                proto.amount,
                proto.timestamp
            )
        }

        override fun toProtoMessage(obj: OffchainTransactionData): Starcoin.OffchainTransactionData {
            return Starcoin.OffchainTransactionData.newBuilder()
                .setEon(obj.eon)
                .setFrom(obj.from.toByteString())
                .setTo(obj.to.toByteString())
                .setAmount(obj.amount)
                .setTimestamp(obj.timestamp).build()
        }
    }
}

@Serializable
@ProtobufSchema(ProtoOffchainTransaction::class)
data class OffchainTransaction(@SerialId(1) var data: OffchainTransactionData, @SerialId(2) var sign: Signature = Signature.ZERO_SIGN) :
    SiriusObject(), MerkleTree.MerkleTreeData {


    constructor(eon: Int, from: Address, to: Address, amount: Long) : this(
        OffchainTransactionData(
            eon,
            from,
            to,
            amount
        )
    )

    @kotlinx.serialization.Transient
    val eon: Int
        get() = data.eon

    @kotlinx.serialization.Transient
    val from: Address
        get() = data.from

    @kotlinx.serialization.Transient
    val to: Address
        get() = data.to

    @kotlinx.serialization.Transient
    val amount: Long
        get() = data.amount

    @kotlinx.serialization.Transient
    val timestamp: Long
        get() = data.timestamp

    fun sign(privateKey: PrivateKey) {
        this.sign = Signature.of(this.data, privateKey)
    }

    fun verify(publicKey: PublicKey): Boolean {
        return when {
            this.data.from == Address.getAddress(publicKey) -> this.sign.verify(this.data, publicKey)
            else -> false
        }
    }

    companion object :
        SiriusObjectCompanion<OffchainTransaction, ProtoOffchainTransaction>(OffchainTransaction::class) {

        override fun mock(): OffchainTransaction {
            return OffchainTransaction(
                RandomUtils.nextInt(),
                CryptoService.getDummyCryptoKey().getAddress(),
                Address.random(),
                RandomUtils.nextLong()
            )
        }

        override fun parseFromProtoMessage(proto: ProtoOffchainTransaction): OffchainTransaction {
            return OffchainTransaction(
                OffchainTransactionData.parseFromProtoMessage(proto.data),
                Signature.wrap(proto.sign)
            )
        }

        override fun toProtoMessage(obj: OffchainTransaction): ProtoOffchainTransaction {
            return ProtoOffchainTransaction.newBuilder().setData(OffchainTransactionData.toProtoMessage(obj.data))
                .setSign(obj.sign.toByteString()).build()
        }
    }
}
