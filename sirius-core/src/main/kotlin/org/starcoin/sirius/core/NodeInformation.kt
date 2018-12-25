package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoNodeInfo::class)
@Serializable
data class NodeInformation(
    @SerialId(1)
    var left: Hash = Hash.ZERO_HASH,
    @SerialId(2)
    var offset: Long = 0,
    @SerialId(3)
    var right: Hash = Hash.ZERO_HASH
) : SiriusObject() {
    companion object : SiriusObjectCompanion<NodeInformation, Starcoin.ProtoNodeInfo>(NodeInformation::class) {

        val EMPTY_NODE = NodeInformation(
            Hash.ZERO_HASH, 0,
            Hash.ZERO_HASH
        )

        fun generateNodeInformation(proto: Starcoin.ProtoNodeInfo?): NodeInformation? {
            if (proto == null) {
                return null
            }
            return NodeInformation(Hash.wrap(proto.left), proto.offset, Hash.wrap(proto.right))
        }

        override fun mock(): NodeInformation {
            return NodeInformation(Hash.random(), 0, Hash.random())
        }
    }
}
