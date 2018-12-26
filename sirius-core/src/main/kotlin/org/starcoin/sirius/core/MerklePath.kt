package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoMerklePathDirection
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils

@ProtobufSchema(Starcoin.ProtoMerklePath::class)
@Serializable
data class MerklePath(@SerialId(1) private val nodes: MutableList<MerklePathNode> = mutableListOf()) : SiriusObject(),
    List<MerklePathNode> by nodes {

    @Transient
    val leafNode: MerklePathNode
        get() = nodes[0]

    fun append(node: MerkleTreeNode) {
        this.nodes.add(MerklePathNode(node.hash(), node.direction))
    }

    fun append(nodeHash: Hash, direction: Direction) {
        this.nodes.add(MerklePathNode(nodeHash, direction))
    }

    fun append(pathNode: MerklePathNode) {
        this.nodes.add(pathNode)
    }

    enum class Direction constructor(private val protoType: ProtoMerklePathDirection) :
        ProtoEnum<ProtoMerklePathDirection> {
        ROOT(ProtoMerklePathDirection.DIRECTION_ROOT),
        LEFT(ProtoMerklePathDirection.DIRECTION_LEFT),
        RIGHT(ProtoMerklePathDirection.DIRECTION_RIGTH);

        override fun toProto(): ProtoMerklePathDirection {
            return protoType
        }

        companion object {

            fun valueOf(number: Int): Direction {
                for (direction in Direction.values()) {
                    if (direction.number == number) {
                        return direction
                    }
                }
                return Direction.ROOT
            }

            fun random(): Direction {
                return Direction.values()[MockUtils.nextInt(0, Direction.values().size)]
            }
        }
    }

    companion object : SiriusObjectCompanion<MerklePath, Starcoin.ProtoMerklePath>(MerklePath::class) {
        override fun mock(): MerklePath {
            val path = MerklePath()
            for (i in 0..MockUtils.nextInt(0, 10)) {
                path.append(MerklePathNode.mock())
            }
            return path
        }
    }

}

@ProtobufSchema(Starcoin.ProtoMerklePathNode::class)
@Serializable
data class MerklePathNode(@SerialId(1) val nodeHash: Hash, @SerialId(2) val direction: MerklePath.Direction) :
    SiriusObject() {

    companion object : SiriusObjectCompanion<MerklePathNode, Starcoin.ProtoMerklePathNode>(MerklePathNode::class) {
        override fun mock(): MerklePathNode {
            return MerklePathNode(Hash.random(), MerklePath.Direction.random())
        }

    }
}
