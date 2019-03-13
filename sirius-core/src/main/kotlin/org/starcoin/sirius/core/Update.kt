package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey


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

    constructor() : this(0, 0L, 0L, 0L)

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
            sendAmount.toBigInteger(),
            receiveAmount.toBigInteger(),
            root
        )
    )

    @JvmOverloads
    constructor(
        eon: Int,
        version: Long,
        sendAmount: BigInteger,
        receiveAmount: BigInteger,
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

    @Transient
    val isSigned: Boolean
        get() = !this.sign.isZero()

    @Transient
    val isSignedByHub: Boolean
        get() = !this.hubSign.isZero()

    @Transient
    val eon: Int
        get() = data.eon

    @Transient
    val version: Long
        get() = data.version

    @Transient
    val sendAmount: BigInteger
        get() = data.sendAmount

    @Transient
    val receiveAmount: BigInteger
        get() = data.receiveAmount

    @Transient
    val root: Hash
        get() = data.root

    fun isEmpty(): Boolean {
        return this.data.isEmpty() && !this.isSigned && !this.isSignedByHub
    }

    fun verfySign(key: CryptoKey) = this.verifySig(key.keyPair.public)

    fun verifySig(publicKey: PublicKey): Boolean {
        return when {
            this.isSigned -> this.sign.verify(data, publicKey)
            else -> false
        }
    }

    fun verifyHubSig(key: CryptoKey) = this.verifyHubSig(key.keyPair.public)

    fun verifyHubSig(publicKey: PublicKey): Boolean {
        return when {
            this.isSignedByHub -> this.hubSign.verify(data, publicKey)
            else -> false
        }
    }

    fun sign(key: CryptoKey) {
        this.sign = key.sign(this.data)
    }

    fun sign(privateKey: PrivateKey) {
        this.sign(CryptoService.loadCryptoKey(privateKey))
    }

    fun signHub(key: CryptoKey) {
        this.hubSign = key.sign(this.data)
    }

    fun signHub(hubPrivateKey: PrivateKey) {
        this.sign(CryptoService.loadCryptoKey(hubPrivateKey))
    }

    companion object : SiriusObjectCompanion<Update, Starcoin.Update>(Update::class) {
        var DUMMY_UPDATE = Update(UpdateData.DUMMY_UPDATE_DATA)

        override fun mock(): Update {
            val update = Update(UpdateData.mock())
            val userKey = CryptoService.generateCryptoKey()
            val hubKey = CryptoService.generateCryptoKey()
            update.sign(userKey)
            update.sign(hubKey)
            return update
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): Update {
            return Update(UpdateData.newUpdate(eon, version, address, txs))
        }
    }
}

@Serializable
@ProtobufSchema(Starcoin.UpdateData::class)
data class UpdateData(
    @SerialId(1)
    var eon: Int = 0,
    @SerialId(2)
    var version: Long = 0,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    var sendAmount: BigInteger = BigInteger.ZERO,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    var receiveAmount: BigInteger = BigInteger.ZERO,
    @SerialId(5)
    var root: Hash = Hash.EMPTY_DADA_HASH
) : SiriusObject() {

    constructor(
        eon: Int,
        version: Long,
        sendAmount: Long,
        receiveAmount: Long,
        root: Hash = Hash.EMPTY_DADA_HASH
    ) : this(eon, version, sendAmount.toBigInteger(), receiveAmount.toBigInteger(), root)

    fun isEmpty(): Boolean {
        return this.version == 0L && this.sendAmount == BigInteger.ZERO && this.receiveAmount == BigInteger.ZERO && root == Hash.EMPTY_DADA_HASH
    }

    companion object : SiriusObjectCompanion<UpdateData, Starcoin.UpdateData>(
        UpdateData::class
    ) {

        var DUMMY_UPDATE_DATA = UpdateData()

        override fun mock(): UpdateData {
            return UpdateData(
                MockUtils.nextInt(),
                MockUtils.nextLong(),
                MockUtils.nextBigInteger(),
                MockUtils.nextBigInteger(),
                Hash.random()
            )
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): UpdateData {
            val tree = MerkleTree(txs)
            val root = tree.hash()
            val sendAmount = txs.stream()
                .filter { transaction -> transaction.from == address }
                .map<BigInteger> { it.amount }
                .reduce(BigInteger.ZERO) { a, b -> a.add(b) }
            val receiveAmount = txs.stream()
                .filter { transaction -> transaction.to == address }
                .map<BigInteger> { it.amount }
                .reduce(BigInteger.ZERO) { a, b -> a.add(b) }
            return UpdateData(eon, version, sendAmount, receiveAmount, root)
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.UpdateData): UpdateData {
            return UpdateData(
                protoMessage.eon,
                protoMessage.version,
                BigInteger(protoMessage.sendAmount.toByteArray()),
                BigInteger(protoMessage.receiveAmount.toByteArray()),
                Hash.wrap(protoMessage.root)
            )
        }

        override fun toProtoMessage(obj: UpdateData): Starcoin.UpdateData {
            return Starcoin.UpdateData.newBuilder()
                .setEon(obj.eon)
                .setVersion(obj.version)
                .setSendAmount(obj.sendAmount.toByteArray().toByteString())
                .setReceiveAmount(obj.receiveAmount.toByteArray().toByteString())
                .setRoot(obj.root.toByteString()).build()
        }
    }
}