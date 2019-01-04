package org.starcoin.sirius.core

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger


abstract class AMTreePathNode : SiriusObject() {
    abstract val nodeInfo: AMTreeNodeInfo
    abstract val direction: PathDirection
    abstract val offset: BigInteger
    abstract val allotment: BigInteger
}

@ProtobufSchema(Starcoin.AMTreePathInternalNode::class)
@Serializable
data class AMTreePathInternalNode(
    @SerialId(1) override val nodeInfo: AMTreeInternalNodeInfo,
    @SerialId(2) override val direction: PathDirection,
    @SerialId(3) @Serializable(with = BigIntegerSerializer::class) override val offset: BigInteger,
    @SerialId(4) @Serializable(with = BigIntegerSerializer::class) override val allotment: BigInteger
) : AMTreePathNode() {
    constructor(nodeInfo: AMTreeInternalNodeInfo, direction: PathDirection, offset: Long, allotment: Long) : this(
        nodeInfo,
        direction,
        offset.toBigInteger(),
        allotment.toBigInteger()
    )

    companion object :
        SiriusObjectCompanion<AMTreePathInternalNode, Starcoin.AMTreePathInternalNode>(AMTreePathInternalNode::class) {
        val DUMMY_NODE = AMTreePathInternalNode(
            AMTreeInternalNodeInfo.DUMMY_NODE,
            PathDirection.ROOT,
            BigInteger.ZERO,
            BigInteger.ZERO
        )

        override fun mock(): AMTreePathInternalNode {
            return AMTreePathInternalNode(
                AMTreeInternalNodeInfo.mock(),
                PathDirection.random(),
                MockUtils.nextBigInteger(),
                MockUtils.nextBigInteger()
            )
        }
    }
}

@ProtobufSchema(Starcoin.AMTreePathLeafNode::class)
@Serializable
data class AMTreePathLeafNode(
    @SerialId(1)
    override val nodeInfo: AMTreeLeafNodeInfo,
    @SerialId(2)
    override val direction: PathDirection,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    override val offset: BigInteger,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    override val allotment: BigInteger
) : AMTreePathNode() {
    constructor(nodeInfo: AMTreeLeafNodeInfo, direction: PathDirection, offset: Long, allotment: Long) : this(
        nodeInfo,
        direction,
        offset.toBigInteger(),
        allotment.toBigInteger()
    )

    companion object :
        SiriusObjectCompanion<AMTreePathLeafNode, Starcoin.AMTreePathLeafNode>(AMTreePathLeafNode::class) {
        val DUMMY_NODE =
            AMTreePathLeafNode(AMTreeLeafNodeInfo.DUMMY_NODE, PathDirection.ROOT, BigInteger.ZERO, BigInteger.ZERO)

        override fun mock(): AMTreePathLeafNode {
            return AMTreePathLeafNode(
                AMTreeLeafNodeInfo.mock(),
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
    val leaf: AMTreePathLeafNode,
    @SerialId(3)
    @Optional
    private val nodes: MutableList<AMTreePathInternalNode> = mutableListOf()
) : List<AMTreePathInternalNode> by nodes, SiriusObject() {
    fun append(node: AMTreeNode) {
        this.nodes.add(node.toAMTreePathNode() as AMTreePathInternalNode)
    }

    fun append(node: AMTreePathInternalNode) {
        this.nodes.add(node)
    }

    companion object : SiriusObjectCompanion<AMTreePath, Starcoin.AMTreePath>(AMTreePath::class) {
        val DUMMY_PATH = AMTreePath(0, AMTreePathLeafNode.DUMMY_NODE)

        override fun mock(): AMTreePath {
            val path = AMTreePath(MockUtils.nextInt(), AMTreePathLeafNode.mock())
            for (i in 0..MockUtils.nextInt(0, 10)) {
                path.append(AMTreePathInternalNode.mock())
            }
            return path
        }
    }

}
