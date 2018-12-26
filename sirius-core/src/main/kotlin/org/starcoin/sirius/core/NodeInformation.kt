package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoNodeInfo::class)
@Serializable
data class NodeInformation(
    @SerialId(1)
    val left: Hash = Hash.ZERO_HASH,
    @SerialId(2)
    val offset: Long = 0,
    @SerialId(3)
    val right: Hash = Hash.ZERO_HASH
) : SiriusObject() {
    companion object : SiriusObjectCompanion<NodeInformation, Starcoin.ProtoNodeInfo>(NodeInformation::class) {

        val EMPTY_NODE = NodeInformation(
            Hash.ZERO_HASH, 0,
            Hash.ZERO_HASH
        )

        override fun parseFromProtoMessage(proto: Starcoin.ProtoNodeInfo): NodeInformation {
            return NodeInformation(Hash.wrap(proto.left), proto.offset, Hash.wrap(proto.right))
        }

        override fun toProtoMessage(obj: NodeInformation): Starcoin.ProtoNodeInfo {
            return Starcoin.ProtoNodeInfo.newBuilder().setLeft(obj.left.toByteString()).setOffset(obj.offset)
                .setRight(obj.right.toByteString()).build()
        }

        @Deprecated("use parseFromProtoMessage", replaceWith = ReplaceWith("parseFromProtoMessage", ""))
        fun generateNodeInformation(proto: Starcoin.ProtoNodeInfo): NodeInformation? {
            return parseFromProtoMessage(proto)
        }

        override fun mock(): NodeInformation {
            return NodeInformation(Hash.random(), RandomUtils.nextLong(), Hash.random())
        }
    }
}
