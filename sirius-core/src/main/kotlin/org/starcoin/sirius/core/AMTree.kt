package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.util.*
import java.util.stream.Collectors

sealed class AMTreeNodeInfo : SiriusObject()

@ProtobufSchema(Starcoin.AMTreeInternalNodeInfo::class)
@Serializable
data class AMTreeInternalNodeInfo(
    @SerialId(1) val left: Hash,
    @SerialId(2) val offset: Long,
    @SerialId(3) val right: Hash
) : AMTreeNodeInfo() {
    constructor(left: Hashable, offset: Long, right: Hashable) : this(left.hash(), offset, right.hash())

    companion object :
        SiriusObjectCompanion<AMTreeInternalNodeInfo, Starcoin.AMTreeInternalNodeInfo>(AMTreeInternalNodeInfo::class) {
        val DUMMY_NODE = AMTreeInternalNodeInfo(Hash.EMPTY_DADA_HASH, 0, Hash.EMPTY_DADA_HASH)

        override fun mock(): AMTreeInternalNodeInfo {
            return AMTreeInternalNodeInfo(Hash.random(), MockUtils.nextLong(), Hash.random())
        }
    }
}

@ProtobufSchema(Starcoin.AMTreeLeafNodeInfo::class)
@Serializable
data class AMTreeLeafNodeInfo(
    @SerialId(1) val addressHash: Hash,
    @SerialId(2) val update: Update
) : AMTreeNodeInfo() {
    companion object :
        SiriusObjectCompanion<AMTreeLeafNodeInfo, Starcoin.AMTreeLeafNodeInfo>(AMTreeLeafNodeInfo::class) {
        val DUMMY_NODE = AMTreeLeafNodeInfo(Hash.EMPTY_DADA_HASH, Update.DUMMY_UPDATE)
        override fun mock(): AMTreeLeafNodeInfo {
            return AMTreeLeafNodeInfo(Hash.random(), Update.mock())
        }
    }
}

@ProtobufSchema(Starcoin.AMTreeProof::class)
@Serializable
data class AMTreeProof(@SerialId(1) val path: AMTreePath, @SerialId(2) val leaf: AMTreePathLeafNode) : SiriusObject() {
    companion object : SiriusObjectCompanion<AMTreeProof, Starcoin.AMTreeProof>(AMTreeProof::class) {
        override fun mock(): AMTreeProof {
            return AMTreeProof(AMTreePath.mock(), AMTreePathLeafNode.mock())
        }
    }
}

/**
 * AugmentedMerkleTree
 */
class AMTree(
    val eon: Int,
    val root: AMTreeNode = AMTreeNode()
) {


    val offset: Long
        get() = this.root.offset

    val allotment: Long
        get() = this.root.allotment

    val info: AMTreeNodeInfo?
        get() = this.root.info

    // just for test.
    val randommProof: AMTreeProof?
        get() = this.getMembershipProof((this.randomLeafNode()?.info as AMTreeLeafNodeInfo?)?.addressHash)


    constructor(eon: Int, accounts: List<HubAccount>) : this(
        eon,
        buildRoot(buildTreeNodes(accounts))
    )

    fun hash(): Hash {
        return this.root.hash()
    }

    fun findTreeNode(nodeHash: Hash): AMTreeNode? {
        return findTreeNode(this.root) { node -> node.hash() == nodeHash }
    }

    fun findLeafNode(address: Address?): AMTreeNode? {
        return this.findLeafNode(address?.hash())
    }

    fun findLeafNode(addressHash: Hash?): AMTreeNode? {
        return findTreeNode(
            this.root
        ) { node -> node.isLeafNode && (node.info as AMTreeLeafNodeInfo).addressHash == addressHash }
    }

    private fun findTreeNode(
        node: AMTreeNode?, predicate: (AMTreeNode) -> Boolean
    ): AMTreeNode? {
        return when {
            node == null -> null
            predicate(node) -> node
            else -> findTreeNode(node.left, predicate) ?: findTreeNode(node.right, predicate)

        }
    }

    fun getMembershipProof(addressHash: Hash?): AMTreeProof? {
        val leaf = this.findLeafNode(addressHash) ?: return null

        var siblingNode = leaf.sibling ?: return null

        val path = AMTreePath(
            this.eon,
            siblingNode.toAMTreePathNode() as AMTreePathLeafNode
        )
        val proof = AMTreeProof(path, leaf.toAMTreePathNode() as AMTreePathLeafNode)

        var parent = leaf.parent ?: return proof
        while (parent.parent != null) {
            siblingNode = parent.sibling ?: return proof
            path.append(siblingNode)
            parent = parent.parent ?: return proof
        }
        return proof
    }

    fun getMembershipProof(address: Address?): AMTreeProof? {
        return this.getMembershipProof(address?.hash())
    }

    fun randomLeafNode(): AMTreeNode? {
        var node = randomChild(this.root) ?: return null
        while (!node.isLeafNode) {
            node = randomChild(node) ?: return null
        }
        return node
    }

    private fun randomChild(node: AMTreeNode): AMTreeNode? {
        return when {
            node.isLeafNode -> node
            MockUtils.nextBoolean() -> node.left
            else -> node.right
        }
    }

    companion object {

        private fun buildTreeNodes(
            accounts: List<HubAccount>
        ): List<AMTreeNode> {
            val prev = arrayOfNulls<AMTreeNode>(1)
            return accounts
                .stream()
                .map { account ->
                    val node = AMTreeNode(
                        prev[0],
                        AMTreeLeafNodeInfo(account.address.hash(), account.update),
                        account.calculateNewAllotment()
                    )
                    prev[0] = node
                    node
                }
                .collect(Collectors.toList())
        }

        private fun buildRoot(leaves: List<AMTreeNode>): AMTreeNode {
            if (leaves.isEmpty()) {
                return AMTreeNode()
            }
            val mergedLeaves = ArrayList<AMTreeNode>()
            var i = 0
            val n = leaves.size
            while (i < n) {
                if (i < n - 1) {
                    mergedLeaves.add(AMTreeNode(leaves[i], leaves[i + 1]))
                    i++
                } else {
                    mergedLeaves.add(AMTreeNode(leaves[i]))
                }
                i++
            }

            return if (mergedLeaves.size > 1) {
                buildRoot(mergedLeaves)
            } else {
                mergedLeaves[0]
            }
        }

        fun buildRoot(path: AMTreePath, leafNode: AMTreePathLeafNode): AMTreeNode {

            var node = if (path.leaf.direction == Direction.LEFT) AMTreeNode(
                AMTreeNode(path.leaf), AMTreeNode(leafNode)
            ) else AMTreeNode(AMTreeNode(leafNode), AMTreeNode(path.leaf))

            for (i in 0 until path.size) {
                val pathNode = path[i]
                node = when {
                    pathNode.direction == Direction.LEFT -> AMTreeNode(
                        AMTreeNode(
                            pathNode
                        ), node
                    )
                    else -> AMTreeNode(
                        node,
                        AMTreeNode(pathNode)
                    )
                }
            }
            return node
        }

        fun verifyMembershipProof(root: AMTreePathInternalNode?, proof: AMTreeProof?): Boolean {
            return verifyMembershipProof(root, proof?.path, proof?.leaf)
        }

        fun verifyMembershipProof(
            root: AMTreeNode?, proof: AMTreeProof?
        ): Boolean {
            return this.verifyMembershipProof(
                root?.toAMTreePathNode() as AMTreePathInternalNode?,
                proof?.path,
                proof?.leaf
            )
        }

        fun verifyMembershipProof(
            root: AMTreeNode?, path: AMTreePath?, leaf: AMTreeNode?
        ): Boolean {
            return this.verifyMembershipProof(
                root?.toAMTreePathNode() as AMTreePathInternalNode?,
                path,
                leaf?.toAMTreePathNode() as AMTreePathLeafNode
            )
        }

        fun verifyMembershipProof(
            root: AMTreePathInternalNode?,
            path: AMTreePath?,
            leaf: AMTreePathLeafNode?
        ): Boolean {
            return when {
                root == null || path == null || leaf == null -> false
                else -> {
                    val buildRoot = buildRoot(path, leaf)
                    return buildRoot.hash() == root.hash() && buildRoot.offset == root.offset && buildRoot.allotment == root.allotment
                }
            }
        }

        fun random(): AMTree {
            return random(MockUtils.nextInt(1, 100))
        }

        fun random(count: Int): AMTree {
            val eon = 1
            val accounts = mutableListOf<HubAccount>()

            for (i in 0 until count) {
                accounts.add(HubAccount.mock())
            }

            return AMTree(eon, accounts)
        }
    }
}

class AMTreeNode(
    val offset: Long = 0,
    val info: AMTreeNodeInfo = AMTreeLeafNodeInfo.DUMMY_NODE,
    val allotment: Long = 0
) : CachedHashable() {

    var parent: AMTreeNode? = null
        private set

    var left: AMTreeNode? = null
        private set

    var right: AMTreeNode? = null
        private set


    constructor(
        prev: AMTreeNode?,
        info: AMTreeLeafNodeInfo,
        allotment: Long
    ) : this(if (prev == null) 0 else prev.offset + prev.allotment, info, allotment)

    constructor(
        left: AMTreeNode, right: AMTreeNode = AMTreeNode(
            left.offset + left.allotment, AMTreeLeafNodeInfo.DUMMY_NODE, 0
        )
    ) : this(left.offset, AMTreeInternalNodeInfo(left, right.offset, right), left.allotment + right.allotment) {
        left.parent = this
        right.parent = this
        this.left = left
        this.right = right
    }

    constructor(pathNode: AMTreePathLeafNode) : this(
        pathNode.offset,
        pathNode.nodeInfo,
        pathNode.allotment
    )

    constructor(pathNode: AMTreePathInternalNode) : this(
        pathNode.offset,
        pathNode.nodeInfo,
        pathNode.allotment
    )

    val isLeafNode: Boolean
        get() = this.left == null && this.right == null && this.info is AMTreeLeafNodeInfo

    val isInternalNode: Boolean
        get() = this.info is AMTreeInternalNodeInfo

    val sibling: AMTreeNode?
        get() {
            return when {
                this.parent == null -> null
                this === this.parent?.left -> this.parent?.right
                else -> this.parent?.left
            }
        }

    val direction: Direction
        get() {
            return when {
                this.parent == null -> Direction.ROOT
                this === this.parent?.left -> Direction.LEFT
                else -> Direction.RIGHT
            }
        }

    override fun doHash(): Hash {
        return Hash.of(this.toAMTreePathNode())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AMTreeNode) return false

        if (offset != other.offset) return false
        if (info != other.info) return false
        if (allotment != other.allotment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + info.hashCode()
        result = 31 * result + allotment.hashCode()
        return result
    }

    fun toAMTreePathNode(): AMTreePathNode {
        return when (this.info) {
            is AMTreeInternalNodeInfo -> AMTreePathInternalNode(
                this.info,
                this.direction,
                this.offset,
                this.allotment
            )
            is AMTreeLeafNodeInfo -> AMTreePathLeafNode(
                this.info,
                this.direction,
                this.offset,
                this.allotment
            )
        }
    }

}

