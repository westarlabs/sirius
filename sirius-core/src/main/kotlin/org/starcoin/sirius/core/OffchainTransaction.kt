package org.starcoin.sirius.core


import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import java.security.PrivateKey
import java.security.PublicKey

@Serializable
@ProtobufSchema(ProtoOffchainTransaction::class)
data class OffchainTransaction(@SerialId(1) val data: OffchainTransactionData, @SerialId(2) var sign: Signature = Signature.ZERO_SIGN) :
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

    override fun doHash(): Hash {
        return this.data.hash()
    }

    fun sign(privateKey: PrivateKey) {
        this.sign = Signature.of(this.data, privateKey)
    }

    fun sign(key: CryptoKey) {
        this.sign(key.getKeyPair().private)
    }

    fun verify(publicKey: PublicKey): Boolean {
        //TODO need verify address at hear?
        //this.data.from == Address.getAddress(publicKey)
        return this.sign.verify(this.data, publicKey)
    }

    fun verify(key: CryptoKey) = verify(key.getKeyPair().public)

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
