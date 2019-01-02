package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.serialization.ProtobufSchema
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
        get() = !this.sign.isZero()

    @kotlinx.serialization.Transient
    val isSignedByHub: Boolean
        get() = !this.hubSign.isZero()

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

    fun sign(key: CryptoKey) = this.sign(key.keyPair.private)

    fun sign(privateKey: PrivateKey) {
        this.sign = Signature.of(this.data, privateKey)
    }

    fun signHub(key: CryptoKey) = this.signHub(key.keyPair.private)

    fun signHub(hubPrivateKey: PrivateKey) {
        this.hubSign = Signature.of(this.data, hubPrivateKey)
    }

    companion object : SiriusObjectCompanion<Update, Starcoin.Update>(Update::class) {
        var DUMMY_UPDATE = Update(UpdateData.DUMMY_UPDATE_DATA)

        override fun mock(): Update {
            return Update(UpdateData.mock(), Signature.random(), Signature.random())
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): Update {
            return Update(UpdateData.newUpdate(eon, version, address, txs))
        }
    }
}
