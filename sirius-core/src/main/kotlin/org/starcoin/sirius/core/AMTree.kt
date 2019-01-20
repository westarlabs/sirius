package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import java.util.*
import java.util.stream.Collectors

sealed class AMTreeNodeInfo : SiriusObject()

@ProtobufSchema(Starcoin.AMTreeInternalNodeInfo::class)
@Serializable
data class AMTreeInternalNodeInfo(
    @SerialId(1) val left: Hash,
    @SerialId(2) @Serializable(with = BigIntegerSerializer::class) val offset: BigInteger,
    @SerialId(3) val right: Hash
) : AMTreeNodeInfo() {
    constructor(left: Hashable, offset: BigInteger, right: Hashable) : this(left.hash(), offset, right.hash())
    constructor(left: Hash, offset: Long, right: Hash) : this(left, offset.toBigInteger(), right)

    companion object :
        SiriusObjectCompanion<AMTreeInternalNodeInfo, Starcoin.AMTreeInternalNodeInfo>(AMTreeInternalNodeInfo::class) {
        val DUMMY_NODE = AMTreeInternalNodeInfo(Hash.EMPTY_DADA_HASH, BigInteger.ZERO, Hash.EMPTY_DADA_HASH)

        override fun mock(): AMTreeInternalNodeInfo {
            return AMTreeInternalNodeInfo(Hash.random(), MockUtils.nextBigInteger(), Hash.random())
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
data class AMTreeProof(@SerialId(1) val path: AMTreePath, @SerialId(2) val leaf: AMTreeLeafNodeInfo) : SiriusObject() {
    companion object : SiriusObjectCompanion<AMTreeProof, Starcoin.AMTreeProof>(AMTreeProof::class) {
        val DUMMY_PROOF = AMTreeProof(AMTreePath.DUMMY_PATH, AMTreeLeafNodeInfo.DUMMY_NODE)
        override fun mock(): AMTreeProof {
            return AMTreeProof(AMTreePath.mock(), AMTreeLeafNodeInfo.mock())
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


    val offset: BigInteger
        get() = this.root.offset

    val allotment: BigInteger
        get() = this.root.allotment

    val info: AMTreeNodeInfo?
        get() = this.root.info

    // just for test.
    val randommProof: AMTreeProof?
        get() = this.getMembershipProof((this.randomLeafNode()?.info as AMTreeLeafNodeInfo?)?.addressHash)

    constructor() : this(0, AMTreeNode.DUMMY_NODE)

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
        ) { it.isLeafNode && (it.info as AMTreeLeafNodeInfo).addressHash == addressHash }
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
            leaf.toAMTreePathNode()
        )
        path.append(siblingNode.toAMTreePathNode())
        val proof = AMTreeProof(path, leaf.info as AMTreeLeafNodeInfo)

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
                return AMTreeNode.DUMMY_NODE
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

        fun verifyMembershipProof(root: AMTreePathNode?, proof: AMTreeProof?): Boolean {
            return verifyMembershipProof(root, proof?.path, proof?.leaf)
        }

        fun verifyMembershipProof(
            root: AMTreePathNode?,
            path: AMTreePath?,
            leaf: AMTreeLeafNodeInfo?
        ): Boolean {
            return when {
                root == null || path == null || leaf == null -> false
                path.isEmpty() -> false
                path.leafNode.nodeHash != leaf.hash() -> false
                path.leafNode.direction == PathDirection.ROOT -> false
                else -> {
                    var currentNode = path.leafNode
                    for ((index, node) in path.withIndex()) {
                        if (node.direction == PathDirection.ROOT || node.direction == currentNode.direction) {
                            return false
                        }
                        val (left, right) = if (node.direction == PathDirection.LEFT) Pair(node, currentNode) else Pair(
                            currentNode,
                            node
                        )
                        val direction =
                            if (index + 1 == path.size) PathDirection.ROOT else PathDirection.reversal(path[index + 1].direction)
                        currentNode = AMTreePathNode(
                            AMTreeInternalNodeInfo(left.nodeHash, right.offset, right.nodeHash).hash(),
                            direction,
                            left.offset,
                            left.allotment + right.allotment
                        )
                    }
                    return root.offset == currentNode.offset && root.allotment == currentNode.allotment && root.nodeHash == currentNode.nodeHash
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
    val offset: BigInteger = BigInteger.ZERO,
    val info: AMTreeNodeInfo = AMTreeLeafNodeInfo.DUMMY_NODE,
    val allotment: BigInteger = BigInteger.ZERO
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
        allotment: BigInteger
    ) : this(prev?.let { prev.offset + prev.allotment } ?: BigInteger.ZERO, info, allotment)

    constructor(
        left: AMTreeNode, right: AMTreeNode = AMTreeNode(
            left.offset + left.allotment, AMTreeLeafNodeInfo.DUMMY_NODE, BigInteger.ZERO
        )
    ) : this(left.offset, AMTreeInternalNodeInfo(left, right.offset, right), left.allotment + right.allotment) {
        left.parent = this
        right.parent = this
        this.left = left
        this.right = right
    }

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

    val direction: PathDirection
        get() {
            return when {
                this.parent == null -> PathDirection.ROOT
                this === this.parent?.left -> PathDirection.LEFT
                else -> PathDirection.RIGHT
            }
        }

    override fun doHash(): Hash {
        return Hash.of(this.info)
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
        return AMTreePathNode(
            this.info.hash(),
            this.direction,
            this.offset,
            this.allotment
        )
    }

    companion object {
        val DUMMY_NODE = AMTreeNode(BigInteger.ZERO, AMTreeInternalNodeInfo.DUMMY_NODE, BigInteger.ZERO)
    }

}

