package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*


class Update(
    var eon: Int = 0,
    var version: Long = 0,
    var sendAmount: Long = 0,
    var receiveAmount: Long = 0,
    var root: Hash = Hash.EMPTY_DADA_HASH,
    var sign: Signature = Signature.ZERO_SIGN,
    var hubSign: Signature = Signature.ZERO_SIGN
) : SiriusObject(), ProtobufCodec<Starcoin.ProtoUpdate> {


    val isSigned: Boolean
        get() = this.sign.isZero()

    val isSignedByHub: Boolean
        get() = this.hubSign.isZero()


    fun marshalSginData(): Starcoin.ProtoUpdate.Builder {
        val builder = Starcoin.ProtoUpdate.newBuilder()
            .setEon(this.eon)
            .setVersion(this.version)
            .setSendAmount(this.sendAmount)
            .setReceiveAmount(this.receiveAmount)
            .setRoot(this.root.toByteString())
        return builder
    }

    override fun marshalProto(): Starcoin.ProtoUpdate {
        val builder = this.marshalSginData()
        if (this.sign != null) {
            builder.sign = this.sign!!.toByteString()
        }
        if (this.hubSign != null) {
            builder.hubSign = this.hubSign!!.toByteString()
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoUpdate) {
        this.eon = proto.eon
        this.version = proto.version
        this.sendAmount = proto.sendAmount
        this.receiveAmount = proto.receiveAmount
        this.root = Hash.wrap(proto.root)
        this.sign = Signature.wrap(proto.sign)
        this.hubSign = Signature.wrap(proto.hubSign)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Update) {
            return false
        }
        val update = o as Update?
        return (eon == update!!.eon
                && version == update.version
                && sendAmount == update.sendAmount
                && receiveAmount == update.receiveAmount
                && root == update.root)
    }

    fun verifySig(publicKey: PublicKey?): Boolean {
        if (publicKey == null) {
            return false
        }
        return if (this.sign == null) {
            false
        } else this.sign!!.verify(this.marshalSginData().build().toByteArray(), publicKey)
    }

    fun verifyHubSig(publicKey: PublicKey?): Boolean {
        if (publicKey == null) {
            return false
        }
        return if (this.hubSign == null) {
            false
        } else this.hubSign!!.verify(this.marshalSginData().build().toByteArray(), publicKey)
    }

    fun sign(privateKey: PrivateKey) {
        // TODO optimize resuse bytebuffer
        this.sign = Signature.of(this.marshalSginData().build().toByteArray(), privateKey)
    }

    fun signHub(hubPrivateKey: PrivateKey) {
        this.hubSign = Signature.of(this.marshalSginData().build().toByteArray(), hubPrivateKey)
    }

    override fun hashCode(): Int {
        return Objects.hash(eon, version, sendAmount, receiveAmount, root)
    }


    companion object {

        fun generateUpdate(protoUpdate: Starcoin.ProtoUpdate?): Update? {
            if (protoUpdate == null) {
                return null
            }
            val update = Update()
            update.unmarshalProto(protoUpdate)
            return update
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): Update {
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
            return Update(eon, version, sendAmount, receiveAmount, root)
        }

        fun unmarshalProto(proto: Starcoin.ProtoUpdate): Update {
            val update = Update()
            update.unmarshalProto(proto)
            return update
        }
    }
}
