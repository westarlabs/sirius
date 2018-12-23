package org.starcoin.sirius.core

import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*


class Update : ProtobufCodec<Starcoin.ProtoUpdate>, Mockable {

    // transaction root hash.

    var eon: Int = 0
    // TODO need two version, one for data structure, another for content update.
    var version: Long = 0
    var sendAmount: Long = 0
    var receiveAmount: Long = 0

    var root: Hash? = null

    var sign: Signature? = null
    var hubSign: Signature? = null

    val isSigned: Boolean
        get() = this.sign != null

    val isSignedByHub: Boolean
        get() = this.hubSign != null

    constructor() {}

    //init by eon.
    constructor(eon: Int) {
        this.eon = eon
        this.version = 0
        this.sendAmount = 0
        this.receiveAmount = 0
        this.root = null
    }

    constructor(eon: Int, version: Long, sendAmount: Long, receiveAmount: Long, root: Hash?) {
        this.eon = eon
        this.version = version
        this.sendAmount = sendAmount
        this.receiveAmount = receiveAmount
        this.root = root
    }

    constructor(eon: Int, version: Long, address: BlockAddress, txs: List<OffchainTransaction>) {
        this.eon = eon
        this.version = version
        val tree = MerkleTree(txs)
        this.root = tree.hash()
        this.sendAmount = txs.stream()
            .filter { transaction -> transaction.from == address }
            .map<Long> { it.amount }
            .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
        this.receiveAmount = txs.stream()
            .filter { transaction -> transaction.to == address }
            .map<Long> { it.amount }
            .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
    }

    constructor(update: Starcoin.ProtoUpdate) {
        this.unmarshalProto(update)
    }

    fun marshalSginData(): Starcoin.ProtoUpdate.Builder {
        val builder = Starcoin.ProtoUpdate.newBuilder()
            .setEon(this.eon)
            .setVersion(this.version)
            .setSendAmount(this.sendAmount)
            .setReceiveAmount(this.receiveAmount)
        if (this.root != null) {
            builder.root = this.root!!.toByteString()
        }
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
        this.root = if (proto.root.isEmpty) null else Hash.wrap(proto.root)
        this.sign = if (proto.sign.isEmpty) null else Signature.wrap(proto.sign)
        this.hubSign = if (proto.hubSign.isEmpty) null else Signature.wrap(proto.hubSign)
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
        this.sign = Signature.of(privateKey, this.marshalSginData().build().toByteArray())
    }

    fun signHub(hubPrivateKey: PrivateKey) {
        this.hubSign = Signature.of(hubPrivateKey, this.marshalSginData().build().toByteArray())
    }

    override fun hashCode(): Int {
        return Objects.hash(eon, version, sendAmount, receiveAmount, root)
    }

    override fun mock(context: MockContext) {
        this.eon = RandomUtils.nextInt()
        this.version = RandomUtils.nextInt().toLong()
        this.sendAmount = RandomUtils.nextInt().toLong()
        this.receiveAmount = RandomUtils.nextInt().toLong()
        this.root = Hash.random()
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
    }
}
