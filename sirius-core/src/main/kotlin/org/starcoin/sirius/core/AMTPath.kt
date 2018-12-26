package org.starcoin.sirius.core

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.MerklePath.Direction
import org.starcoin.sirius.serialization.ProtobufSchema


abstract class AMTPathNode : SiriusObject() {
    abstract val nodeInfo: AMTNodeInfo
    abstract val direction: Direction
    abstract val offset: Long
    abstract val allotment: Long
}

@ProtobufSchema(Starcoin.AMTPathInternalNode::class)
@Serializable
data class AMTPathInternalNode(
    @SerialId(1) override val nodeInfo: AMTInternalNodeInfo,
    @SerialId(2) override val direction: Direction,
    @SerialId(3) override val offset: Long,
    @SerialId(4) override val allotment: Long
) : AMTPathNode() {
    companion object :
        SiriusObjectCompanion<AMTPathInternalNode, Starcoin.AMTPathInternalNode>(AMTPathInternalNode::class) {
        val DUMMY_NODE = AMTPathInternalNode(AMTInternalNodeInfo.DUMMY_NODE, Direction.ROOT, 0, 0)
        override fun mock(): AMTPathInternalNode {
            return AMTPathInternalNode(
                AMTInternalNodeInfo.mock(),
                Direction.random(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong()
            )
        }
    }
}

@ProtobufSchema(Starcoin.AMTPathLeafNode::class)
@Serializable
data class AMTPathLeafNode(
    @SerialId(1)
    override val nodeInfo: AMTLeafNodeInfo,
    @SerialId(2)
    override val direction: Direction,
    @SerialId(3)
    override val offset: Long,
    @SerialId(4)
    override val allotment: Long
) : AMTPathNode() {
    companion object : SiriusObjectCompanion<AMTPathLeafNode, Starcoin.AMTPathLeafNode>(AMTPathLeafNode::class) {
        val DUMMY_NODE = AMTPathLeafNode(AMTLeafNodeInfo.DUMMY_NODE, Direction.ROOT, 0, 0)
        override fun mock(): AMTPathLeafNode {
            return AMTPathLeafNode(
                AMTLeafNodeInfo.mock(),
                Direction.random(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong()
            )
        }
    }
}

@ProtobufSchema(Starcoin.AMTPath::class)
@Serializable
data class AMTPath(
    @SerialId(1)
    val eon: Int,
    @SerialId(2)
    val leaf: AMTPathLeafNode,
    @SerialId(3)
    @Optional
    private val nodes: MutableList<AMTPathInternalNode> = mutableListOf()
) : List<AMTPathInternalNode> by nodes, SiriusObject() {


    fun append(node: AMTNode) {
        this.nodes.add(node.toAMTPathNode() as AMTPathInternalNode)
    }

    fun append(node: AMTPathInternalNode) {
        this.nodes.add(node)
    }

    companion object : SiriusObjectCompanion<AMTPath, Starcoin.AMTPath>(AMTPath::class) {
        val DUMMY_PATH = AMTPath(0, AMTPathLeafNode.DUMMY_NODE)

        override fun mock(): AMTPath {
            val path = AMTPath(RandomUtils.nextInt(), AMTPathLeafNode.mock())
            for (i in 0..RandomUtils.nextInt(0, 10)) {
                path.append(AMTPathInternalNode.mock())
            }
            return path
        }
    }

}
