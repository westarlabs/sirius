package org.starcoin.sirius.core


import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey

@Serializable
@ProtobufSchema(Starcoin.OffchainTransaction::class)
data class OffchainTransaction(@SerialId(1) val data: OffchainTransactionData, @SerialId(2) var sign: Signature = Signature.ZERO_SIGN) :
    SiriusObject() {

    constructor(eon: Int, from: Address, to: Address, amount: BigInteger) : this(
        OffchainTransactionData(
            eon,
            from,
            to,
            amount
        )
    )

    constructor(eon: Int, from: Address, to: Address, amount: Long) : this(eon, from, to, amount.toBigInteger())

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
    val amount: BigInteger
        get() = data.amount

    @kotlinx.serialization.Transient
    val timestamp: Long
        get() = data.timestamp

    override fun doHash(): Hash {
        return this.data.hash()
    }

    fun sign(privateKey: PrivateKey) {
        this.sign(CryptoService.loadCryptoKey(privateKey))
    }

    fun sign(key: CryptoKey) {
        this.sign = key.sign(this.data)
    }

    fun verify(publicKey: PublicKey): Boolean {
        //TODO need verify address at hear?
        //this.data.from == Address.getAddress(publicKey)
        return this.sign.verify(this.data, publicKey)
    }

    fun verify(key: CryptoKey) = key.verify(this.data, this.sign)

    companion object :
        SiriusObjectCompanion<OffchainTransaction, Starcoin.OffchainTransaction>(OffchainTransaction::class) {

        var DUMMY_OFFCHAIN_TRAN = OffchainTransaction(OffchainTransactionData.DUMMY_OFFCHAIN_TRAN_DATA)

        override fun mock(): OffchainTransaction {
            return OffchainTransaction(
                OffchainTransactionData(
                    MockUtils.nextInt(),
                    CryptoService.dummyCryptoKey.address,
                    Address.random(),
                    MockUtils.nextLong()
                ), Signature.random()
            )
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.OffchainTransaction): OffchainTransaction {
            return OffchainTransaction(
                OffchainTransactionData.parseFromProtoMessage(protoMessage.data),
                Signature.wrap(protoMessage.sign)
            )
        }

        override fun toProtoMessage(obj: OffchainTransaction): Starcoin.OffchainTransaction {
            return Starcoin.OffchainTransaction.newBuilder().setData(OffchainTransactionData.toProtoMessage(obj.data))
                .setSign(obj.sign.toByteString()).build()
        }
    }
}
