package org.starcoin.sirius.core


import org.starcoin.proto.Starcoin

import java.util.Objects

class NodeInformation : ProtobufCodec<Starcoin.ProtoNodeInfo> {

    var left: Hash? = null
        private set
    var offset: Long = 0
        private set
    var right: Hash? = null
        private set

    constructor() {}

    constructor(left: Hash, offset: Long, right: Hash) {
        this.left = left
        this.offset = offset
        this.right = right
    }

    override fun marshalProto(): Starcoin.ProtoNodeInfo {
        return Starcoin.ProtoNodeInfo.newBuilder()
            .setLeft(this.left!!.toByteString())
            .setOffset(this.offset)
            .setRight(this.right!!.toByteString())
            .build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoNodeInfo) {
        this.left = Hash.wrap(proto.left)
        this.offset = proto.offset
        this.right = Hash.wrap(proto.right)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is NodeInformation) {
            return false
        }
        val that = o as NodeInformation?
        return offset == that!!.offset &&
                left == that.left &&
                right == that.right
    }

    override fun hashCode(): Int {
        return Objects.hash(left, offset, right)
    }

    companion object {

        val EMPTY_NODE = NodeInformation(
            Hash.ZERO_HASH, 0,
            Hash.ZERO_HASH
        )

        fun generateNodeInformation(proto: Starcoin.ProtoNodeInfo?): NodeInformation? {
            if (proto == null) {
                return null
            }
            val nodeInformation = NodeInformation()
            nodeInformation.unmarshalProto(proto)
            return nodeInformation
        }
    }
}
