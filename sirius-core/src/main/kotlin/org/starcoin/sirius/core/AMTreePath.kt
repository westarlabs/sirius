package org.starcoin.sirius.core

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger


@ProtobufSchema(Starcoin.AMTreePathNode::class)
@Serializable
data class AMTreePathNode(
    @SerialId(1) val nodeHash: Hash,
    @SerialId(2) val direction: PathDirection,
    @SerialId(3) @Serializable(with = BigIntegerSerializer::class) val offset: BigInteger,
    @SerialId(4) @Serializable(with = BigIntegerSerializer::class) val allotment: BigInteger
) : SiriusObject() {

    constructor(nodeHash: Hash, direction: PathDirection, offset: Long, allotment: Long) : this(
        nodeHash,
        direction,
        offset.toBigInteger(),
        allotment.toBigInteger()
    )

    companion object :
        SiriusObjectCompanion<AMTreePathNode, Starcoin.AMTreePathNode>(AMTreePathNode::class) {
        val DUMMY_NODE = AMTreePathNode(
            Hash.EMPTY_DADA_HASH,
            PathDirection.ROOT,
            BigInteger.ZERO,
            BigInteger.ZERO
        )

        override fun mock(): AMTreePathNode {
            return AMTreePathNode(
                Hash.random(),
                PathDirection.random(),
                MockUtils.nextBigInteger(),
                MockUtils.nextBigInteger()
            )
        }
    }
}

@ProtobufSchema(Starcoin.AMTreePath::class)
@Serializable
data class AMTreePath(
    @SerialId(1)
    val eon: Int,
    @SerialId(2)
    val leafNode: AMTreePathNode,
    @SerialId(3)
    @Optional
    private val nodes: MutableList<AMTreePathNode> = mutableListOf()
) : SiriusObject(), List<AMTreePathNode> by nodes {

    fun append(node: AMTreeNode) {
        this.nodes.add(node.toAMTreePathNode())
    }

    fun append(node: AMTreePathNode) {
        this.nodes.add(node)
    }

    companion object : SiriusObjectCompanion<AMTreePath, Starcoin.AMTreePath>(AMTreePath::class) {
        val DUMMY_PATH = AMTreePath(0, AMTreePathNode.DUMMY_NODE)

        override fun mock(): AMTreePath {
            val path = AMTreePath(MockUtils.nextInt(), AMTreePathNode.mock())
            for (i in 0..MockUtils.nextInt(0, 10)) {
                path.append(AMTreePathNode.mock())
            }
            return path
        }
    }

}
