package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode
import org.starcoin.proto.Starcoin.ProtoHubInfo
import org.starcoin.sirius.util.KeyPairUtil

import java.security.PublicKey
import java.util.Objects

class HubInfo : ProtobufCodec<ProtoHubInfo> {

    var isReady: Boolean = false
        private set
    var blocksPerEon: Int = 0
        private set
    var eon: Int = 0
        private set
    var root: AugmentedMerkleTreeNode? = null
        private set
    var publicKey: PublicKey? = null
        private set


    constructor() {}

    constructor(ready: Boolean, blocksPerEon: Int) {
        this.isReady = ready
        this.blocksPerEon = blocksPerEon
    }

    constructor(
        ready: Boolean, blocksPerEon: Int, eon: Int,
        root: AugmentedMerkleTreeNode, publicKey: PublicKey
    ) {
        this.isReady = ready
        this.blocksPerEon = blocksPerEon
        this.eon = eon
        this.root = root
        this.publicKey = publicKey
    }

    constructor(proto: ProtoHubInfo) {
        this.unmarshalProto(proto)
    }

    override fun marshalProto(): ProtoHubInfo {
        val builder = ProtoHubInfo.newBuilder().setReady(isReady).setBlocksPerEon(blocksPerEon)
        if (this.isReady) {
            builder
                .setEon(eon)
                .setRoot(root!!.toProto()).publicKey =
                    ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey!!))
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: ProtoHubInfo) {
        this.isReady = proto.ready
        this.eon = proto.eon
        this.root = if (proto.hasRoot()) AugmentedMerkleTreeNode(proto.root) else null
        this.publicKey = if (proto.publicKey.isEmpty)
            null
        else
            KeyPairUtil.recoverPublicKey(proto.publicKey.toByteArray())
        this.blocksPerEon = proto.blocksPerEon
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is HubInfo) {
            return false
        }
        val hubInfo = o as HubInfo?
        return (isReady == hubInfo!!.isReady
                && eon == hubInfo.eon
                && blocksPerEon == hubInfo.blocksPerEon
                && root == hubInfo.root
                && publicKey == hubInfo.publicKey)
    }

    override fun hashCode(): Int {
        return Objects.hash(isReady, eon, root, publicKey, blocksPerEon)
    }

    override fun toString(): String {
        return this.toJson()
    }
}
