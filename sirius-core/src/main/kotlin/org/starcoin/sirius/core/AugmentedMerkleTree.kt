package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import java.util.*
import java.util.stream.Collectors

sealed class AMTNodeInfo : SiriusObject()

@ProtobufSchema(Starcoin.AMTInternalNodeInfo::class)
@Serializable
data class AMTInternalNodeInfo(
    @SerialId(1) val left: Hash,
    @SerialId(2) val offset: Long,
    @SerialId(3) val right: Hash
) : AMTNodeInfo() {
    constructor(left: Hashable, offset: Long, right: Hashable) : this(left.hash(), offset, right.hash())

    companion object :
        SiriusObjectCompanion<AMTInternalNodeInfo, Starcoin.AMTInternalNodeInfo>(AMTInternalNodeInfo::class) {
        val DUMMY_NODE = AMTInternalNodeInfo(Hash.EMPTY_DADA_HASH, 0, Hash.EMPTY_DADA_HASH)

        override fun mock(): AMTInternalNodeInfo {
            return AMTInternalNodeInfo(Hash.random(), RandomUtils.nextLong(), Hash.random())
        }
    }
}

@ProtobufSchema(Starcoin.AMTLeafNodeInfo::class)
@Serializable
data class AMTLeafNodeInfo(
    @SerialId(1) val addressHash: Hash,
    @SerialId(2) val update: Update
) : AMTNodeInfo() {
    companion object : SiriusObjectCompanion<AMTLeafNodeInfo, Starcoin.AMTLeafNodeInfo>(AMTLeafNodeInfo::class) {
        val DUMMY_NODE = AMTLeafNodeInfo(Hash.EMPTY_DADA_HASH, Update.DUMMY_UPDATE)
        override fun mock(): AMTLeafNodeInfo {
            return AMTLeafNodeInfo(Hash.random(), Update.mock())
        }
    }
}

@ProtobufSchema(Starcoin.AMTProof::class)
@Serializable
data class AMTProof(@SerialId(1) val path: AMTPath, @SerialId(2) val leaf: AMTPathLeafNode) : SiriusObject() {
    companion object : SiriusObjectCompanion<AMTProof, Starcoin.AMTProof>(AMTProof::class) {
        override fun mock(): AMTProof {
            return AMTProof(AMTPath.mock(), AMTPathLeafNode.mock())
        }
    }
}

class AugmentedMerkleTree(
    val eon: Int,
    val root: AMTNode = AMTNode()
) {


    val offset: Long
        get() = this.root.offset

    val allotment: Long
        get() = this.root.allotment

    val info: AMTNodeInfo?
        get() = this.root.info

    // just for test.
    val randommProof: AMTProof?
        get() = this.getMembershipProof((this.randomLeafNode()?.info as AMTLeafNodeInfo?)?.addressHash)


    constructor(eon: Int, accounts: List<HubAccount>) : this(
        eon,
        buildRoot(buildTreeNodes(accounts))
    )

    fun hash(): Hash {
        return this.root.hash()
    }

    fun findTreeNode(nodeHash: Hash): AMTNode? {
        return findTreeNode(this.root) { node -> node.hash() == nodeHash }
    }

    fun findLeafNode(address: Address?): AMTNode? {
        return this.findLeafNode(address?.hash())
    }

    fun findLeafNode(addressHash: Hash?): AMTNode? {
        return findTreeNode(
            this.root
        ) { node -> node.isLeafNode && (node.info as AMTLeafNodeInfo).addressHash == addressHash }
    }

    private fun findTreeNode(
        node: AMTNode?, predicate: (AMTNode) -> Boolean
    ): AMTNode? {
        return when {
            node == null -> null
            predicate(node) -> node
            else -> findTreeNode(node.left, predicate) ?: findTreeNode(node.right, predicate)

        }
    }

    fun getMembershipProof(addressHash: Hash?): AMTProof? {
        val leaf = this.findLeafNode(addressHash) ?: return null

        var siblingNode = leaf.sibling ?: return null

        val path = AMTPath(
            this.eon,
            siblingNode.toAMTPathNode() as AMTPathLeafNode
        )
        val proof = AMTProof(path, leaf.toAMTPathNode() as AMTPathLeafNode)

        var parent = leaf.parent ?: return proof
        while (parent.parent != null) {
            siblingNode = parent.sibling ?: return proof
            path.append(siblingNode)
            parent = parent.parent ?: return proof
        }
        return proof
    }

    fun getMembershipProof(address: Address?): AMTProof? {
        return this.getMembershipProof(address?.hash())
    }

    fun randomLeafNode(): AMTNode? {
        var node = randomChild(this.root) ?: return null
        while (!node.isLeafNode) {
            node = randomChild(node) ?: return null
        }
        return node
    }

    private fun randomChild(node: AMTNode): AMTNode? {
        return when {
            node.isLeafNode -> node
            RandomUtils.nextBoolean() -> node.left
            else -> node.right
        }
    }

    companion object {

        private fun buildTreeNodes(
            accounts: List<HubAccount>
        ): List<AMTNode> {
            val prev = arrayOfNulls<AMTNode>(1)
            return accounts
                .stream()
                .map { account ->
                    val node = AMTNode(
                        prev[0],
                        AMTLeafNodeInfo(account.address.hash(), account.update),
                        account.allotment
                    )
                    prev[0] = node
                    node
                }
                .collect(Collectors.toList())
        }

        private fun buildRoot(leaves: List<AMTNode>): AMTNode {
            if (leaves.isEmpty()) {
                return AMTNode()
            }
            val mergedLeaves = ArrayList<AMTNode>()
            var i = 0
            val n = leaves.size
            while (i < n) {
                if (i < n - 1) {
                    mergedLeaves.add(AMTNode(leaves[i], leaves[i + 1]))
                    i++
                } else {
                    mergedLeaves.add(AMTNode(leaves[i]))
                }
                i++
            }

            return if (mergedLeaves.size > 1) {
                buildRoot(mergedLeaves)
            } else {
                mergedLeaves[0]
            }
        }

        fun buildRoot(path: AMTPath, leafNode: AMTPathLeafNode): AMTNode {

            var node = if (path.leaf.direction == MerklePath.Direction.LEFT) AMTNode(
                AMTNode(path.leaf), AMTNode(leafNode)
            ) else AMTNode(AMTNode(leafNode), AMTNode(path.leaf))

            for (i in 0 until path.size) {
                val pathNode = path[i]
                node = when {
                    pathNode.direction == MerklePath.Direction.LEFT -> AMTNode(
                        AMTNode(
                            pathNode
                        ), node
                    )
                    else -> AMTNode(
                        node,
                        AMTNode(pathNode)
                    )
                }
            }
            return node
        }

        fun verifyMembershipProof(root: AMTPathInternalNode?, proof: AMTProof?): Boolean {
            return verifyMembershipProof(root, proof?.path, proof?.leaf)
        }

        fun verifyMembershipProof(
            root: AMTNode?, proof: AMTProof?
        ): Boolean {
            return this.verifyMembershipProof(root?.toAMTPathNode() as AMTPathInternalNode?, proof?.path, proof?.leaf)
        }

        fun verifyMembershipProof(
            root: AMTNode?, path: AMTPath?, leaf: AMTNode?
        ): Boolean {
            return this.verifyMembershipProof(
                root?.toAMTPathNode() as AMTPathInternalNode?,
                path,
                leaf?.toAMTPathNode() as AMTPathLeafNode
            )
        }

        fun verifyMembershipProof(root: AMTPathInternalNode?, path: AMTPath?, leaf: AMTPathLeafNode?): Boolean {
            return when {
                root == null || path == null || leaf == null -> false
                else -> {
                    val buildRoot = buildRoot(path, leaf)
                    return buildRoot.hash() == root.hash() && buildRoot.offset == root.offset && buildRoot.allotment == root.allotment
                }
            }
        }

        fun random(): AugmentedMerkleTree {
            return random(RandomUtils.nextInt(1, 100))
        }

        fun random(count: Int): AugmentedMerkleTree {
            val eon = 1
            val accounts = mutableListOf<HubAccount>()

            for (i in 0 until count) {
                accounts.add(HubAccount.mock())
            }

            return AugmentedMerkleTree(eon, accounts)
        }
    }
}

class AMTNode(
    val offset: Long = 0,
    val info: AMTNodeInfo = AMTLeafNodeInfo.DUMMY_NODE,
    val allotment: Long = 0
) : CachedHash() {

    var parent: AMTNode? = null
        private set

    var left: AMTNode? = null
        private set

    var right: AMTNode? = null
        private set


    constructor(
        prev: AMTNode?,
        info: AMTLeafNodeInfo,
        allotment: Long
    ) : this(if (prev == null) 0 else prev.offset + prev.allotment, info, allotment)

    constructor(
        left: AMTNode, right: AMTNode = AMTNode(
            left.offset + left.allotment, AMTLeafNodeInfo.DUMMY_NODE, 0
        )
    ) : this(left.offset, AMTInternalNodeInfo(left, right.offset, right), left.allotment + right.allotment) {
        left.parent = this
        right.parent = this
        this.left = left
        this.right = right
    }

    constructor(pathNode: AMTPathLeafNode) : this(
        pathNode.offset,
        pathNode.nodeInfo,
        pathNode.allotment
    )

    constructor(pathNode: AMTPathInternalNode) : this(
        pathNode.offset,
        pathNode.nodeInfo,
        pathNode.allotment
    )

    val isLeafNode: Boolean
        get() = this.left == null && this.right == null && this.info is AMTLeafNodeInfo

    val isInternalNode: Boolean
        get() = this.info is AMTInternalNodeInfo

    val sibling: AMTNode?
        get() {
            return when {
                this.parent == null -> null
                this === this.parent?.left -> this.parent?.right
                else -> this.parent?.left
            }
        }

    val direction: MerklePath.Direction
        get() {
            return when {
                this.parent == null -> MerklePath.Direction.ROOT
                this === this.parent?.left -> MerklePath.Direction.LEFT
                else -> MerklePath.Direction.RIGHT
            }
        }

    override fun doHash(): Hash {
        return Hash.of(this.toAMTPathNode())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AMTNode) return false

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

    fun toAMTPathNode(): AMTPathNode {
        return when (this.info) {
            is AMTInternalNodeInfo -> AMTPathInternalNode(
                this.info,
                this.direction,
                this.offset,
                this.allotment
            )
            is AMTLeafNodeInfo -> AMTPathLeafNode(
                this.info,
                this.direction,
                this.offset,
                this.allotment
            )
        }
    }

}

