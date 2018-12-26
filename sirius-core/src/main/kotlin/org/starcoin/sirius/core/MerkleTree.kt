package org.starcoin.sirius.core

import org.starcoin.sirius.util.MockUtils
import java.util.stream.Collectors

class MerkleTree(private val root: MerkleTreeNode) : Hashable {

    constructor(leaves: List<Hashable>) : this(buildRoot(buildTreeNodes(leaves)))

    constructor(path: MerklePath) : this(buildRoot(path))


    fun getRoot(): MerkleTreeNode {
        return this.root
    }

    override fun hash(): Hash {
        return root.hash()
    }

    fun randomLeafNode(): MerkleTreeNode? {
        var node = this.root.randomChild() ?: return null
        while (!node.isLeafNode) {
            node = node.randomChild() ?: return null
        }
        return node
    }

    fun findTreeNode(nodeHash: Hash?): MerkleTreeNode? {
        return when (nodeHash) {
            null -> null
            else -> findTreeNode(this.root) { node -> node.hash() == nodeHash }
        }
    }

    private fun findTreeNode(
        node: MerkleTreeNode?, predicate: (MerkleTreeNode) -> Boolean
    ): MerkleTreeNode? {
        return when {
            node == null -> null
            predicate(node) -> node
            else -> {
                findTreeNode(node.left, predicate) ?: findTreeNode(node.right, predicate)
            }
        }
    }

    fun getMembershipProof(nodeHash: Hash?): MerklePath? {
        val node = this.findTreeNode(nodeHash) ?: return null
        val path = MerklePath(mutableListOf(MerklePathNode(node.hash(), node.direction)))
        var siblingNode: MerkleTreeNode = node.sibling ?: return null
        path.append(siblingNode)

        var parent = node.parent ?: return path
        while (parent.parent != null) {
            siblingNode = parent.sibling ?: return path
            path.append(siblingNode)
            parent = parent.parent ?: return path
        }
        return path
    }


    companion object {

        private fun buildRoot(leaves: List<MerkleTreeNode>): MerkleTreeNode {
            val mergedLeaves = mutableListOf<MerkleTreeNode>()
            var i = 0
            val n = leaves.size
            while (i < n) {
                if (i < n - 1) {
                    mergedLeaves.add(MerkleTreeNode(leaves[i], leaves[i + 1]))
                    i++
                } else {
                    mergedLeaves.add(MerkleTreeNode(leaves[i], MerkleTreeNode.NULL_TREE_NODE))
                }
                i++
            }

            return if (mergedLeaves.size > 1) {
                buildRoot(mergedLeaves)
            } else {
                mergedLeaves[0]
            }
        }

        private fun buildTreeNodes(datas: List<Hashable>): List<MerkleTreeNode> {
            return datas
                .stream()
                .map { data ->
                    val node = MerkleTreeNode(data)
                    node
                }
                .collect(Collectors.toList())
        }

        private fun buildRoot(path: MerklePath): MerkleTreeNode {
            var node = MerkleTreeNode(path[0].nodeHash)

            for (i in 1 until path.size) {
                val pathNode = path[i]
                node = when {
                    pathNode.direction == MerklePath.Direction.LEFT -> MerkleTreeNode(
                        MerkleTreeNode(pathNode.nodeHash),
                        node
                    )
                    else -> MerkleTreeNode(
                        node,
                        MerkleTreeNode(pathNode.nodeHash)
                    )
                }
            }
            return node
        }

        fun verifyMembershipProof(root: MerkleTreeNode?, path: MerklePath?, leaf: Hashable?): Boolean {
            return verifyMembershipProof(root?.hash(), path, leaf)
        }

        fun verifyMembershipProof(rootHash: Hash?, path: MerklePath?, leaf: Hashable?): Boolean {
            return when {
                leaf == null || path == null || rootHash == null -> false
                leaf.hash() == path.leafNode.nodeHash -> buildRoot(path).hash() == rootHash
                else -> false
            }
        }
    }
}

class MerkleTreeNode(val data: Hashable?, val left: MerkleTreeNode?, var right: MerkleTreeNode?) :
    CachedHash() {

    var parent: MerkleTreeNode? = null
        private set

    constructor(data: Hashable) : this(data, null, null)

    constructor(left: MerkleTreeNode, right: MerkleTreeNode) : this(null, left, right) {
        this.left?.parent = this
        this.right?.parent = this
    }

    val sibling: MerkleTreeNode?
        get() {
            return when {
                this.parent == null -> null
                else -> if (this === this.parent?.left)
                    this.parent?.right
                else
                    this.parent?.left
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

    val isLeafNode: Boolean
        get() = this.data != null

    override fun doHash(): Hash {
        return this.data?.hash() ?: Hash.combine(left?.hash(), right?.hash())
    }

    internal fun randomChild(): MerkleTreeNode? {
        if (this.isLeafNode) {
            return this
        }
        return if (MockUtils.nextBoolean()) {
            this.left
        } else {
            this.right
        }
    }

    companion object {
        val NULL_TREE_NODE = MerkleTreeNode(Hash.EMPTY_DADA_HASH)
    }
}
