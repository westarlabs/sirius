package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.serialization.ProtobufSchema
import java.security.PrivateKey
import java.security.PublicKey


@Serializable
@ProtobufSchema(Starcoin.UpdateData::class)
data class UpdateData(
    @SerialId(1)
    var eon: Int = 0,
    @SerialId(2)
    var version: Long = 0,
    @SerialId(3)
    var sendAmount: Long = 0,
    @SerialId(4)
    var receiveAmount: Long = 0,
    @SerialId(5)
    var root: Hash = Hash.EMPTY_DADA_HASH
) : SiriusObject() {

    companion object : SiriusObjectCompanion<UpdateData, Starcoin.UpdateData>(
        UpdateData::class
    ) {

        override fun mock(): UpdateData {
            return UpdateData(
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                Hash.random()
            )
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): UpdateData {
            val tree = MerkleTree(txs)
            val root = tree.hash()
            val sendAmount = txs.stream()
                .filter { transaction -> transaction.from == address }
                .map<Long> { it.amount }
                .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
            val receiveAmount = txs.stream()
                .filter { transaction -> transaction.to == address }
                .map<Long> { it.amount }
                .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
            return UpdateData(eon, version, sendAmount, receiveAmount, root)
        }

        override fun parseFromProtoMessage(proto: Starcoin.UpdateData): UpdateData {
            return UpdateData(
                proto.eon,
                proto.version,
                proto.sendAmount,
                proto.receiveAmount,
                Hash.wrap(proto.root)
            )
        }

        override fun toProtoMessage(obj: UpdateData): Starcoin.UpdateData {
            return Starcoin.UpdateData.newBuilder()
                .setEon(obj.eon)
                .setVersion(obj.version)
                .setSendAmount(obj.sendAmount)
                .setReceiveAmount(obj.receiveAmount)
                .setRoot(obj.root.toByteString()).build()
        }
    }
}

@ProtobufSchema(Starcoin.Update::class)
@Serializable
data class Update(
    @SerialId(1)
    val data: UpdateData,
    @SerialId(2)
    var sign: Signature = Signature.ZERO_SIGN,
    @SerialId(3)
    var hubSign: Signature = Signature.ZERO_SIGN
) : SiriusObject() {

    constructor(
        eon: Int,
        version: Long,
        sendAmount: Long,
        receiveAmount: Long,
        root: Hash = Hash.EMPTY_DADA_HASH
    ) : this(
        UpdateData(
            eon,
            version,
            sendAmount,
            receiveAmount,
            root
        )
    )

    @kotlinx.serialization.Transient
    val isSigned: Boolean
        get() = this.sign.isZero()

    @kotlinx.serialization.Transient
    val isSignedByHub: Boolean
        get() = this.hubSign.isZero()

    @kotlinx.serialization.Transient
    val eon: Int
        get() = data.eon

    @kotlinx.serialization.Transient
    val version: Long
        get() = data.version

    @kotlinx.serialization.Transient
    val sendAmount: Long
        get() = data.sendAmount

    @kotlinx.serialization.Transient
    val receiveAmount: Long
        get() = data.receiveAmount

    @kotlinx.serialization.Transient
    val root: Hash
        get() = data.root

    fun verfySign(key: CryptoKey) = this.verifySig(key.getKeyPair().public)

    fun verifySig(publicKey: PublicKey): Boolean {
        return when {
            this.isSigned -> this.sign.verify(data, publicKey)
            else -> false
        }
    }

    fun verifyHubSig(key: CryptoKey) = this.verifyHubSig(key.getKeyPair().public)

    fun verifyHubSig(publicKey: PublicKey): Boolean {
        return when {
            this.isSignedByHub -> this.hubSign.verify(data, publicKey)
            else -> false
        }
    }

    fun sign(key: CryptoKey) = this.sign(key.getKeyPair().private)

    fun sign(privateKey: PrivateKey) {
        this.sign = Signature.of(this.data, privateKey)
    }

    fun signHub(key: CryptoKey) = this.signHub(key.getKeyPair().private)

    fun signHub(hubPrivateKey: PrivateKey) {
        this.hubSign = Signature.of(this.data, hubPrivateKey)
    }

    companion object : SiriusObjectCompanion<Update, Starcoin.Update>(Update::class) {
        override fun mock(): Update {
            return Update(UpdateData.mock(), Signature.random(), Signature.random())
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): Update {
            return Update(UpdateData.newUpdate(eon, version, address, txs))
        }
    }
}
