package org.starcoin.sirius.core

import org.apache.commons.lang3.RandomUtils
import org.starcoin.sirius.core.MerklePath.Direction
import org.starcoin.proto.Starcoin.ProtoAugmentedMerkleTreeNode

import java.util.ArrayList
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

class AugmentedMerkleTree {

    var eon = 1
        private set
    var root: AugmentedMerkleTreeNode? = null
        private set

    val offset: Long
        get() = this.root!!.offset

    val allotment: Long
        get() = this.root!!.allotment

    val information: NodeInformation?
        get() = this.root!!.information

    // just for test.
    val randommProof: AugmentedMerklePath?
        get() = this.getMembershipProof(this.randomLeafNode().account!!.address)

    class AugmentedMerkleTreeNode : Hashable, ProtobufCodec<ProtoAugmentedMerkleTreeNode> {

        @Transient
        var parent: AugmentedMerkleTreeNode? = null
            private set
        @Transient
        var left: AugmentedMerkleTreeNode? = null
        @Transient
        var right: AugmentedMerkleTreeNode? = null
        @Transient
        private var hash: Hash? = null

        var offset: Long = 0
            private set
        var information: NodeInformation? = null
            private set
        var account: AccountInformation? = null
            private set
        var allotment: Long = 0
            private set

        val isLeafNode: Boolean
            get() = this.left == null && this.right == null && this.account != null

        val isNnternalNode: Boolean
            get() = this.information != null

        val sibling: AugmentedMerkleTreeNode?
            get() {
                if (this.parent == null) {
                    return null
                }
                return if (this === this.parent!!.left) this.parent!!.right else this.parent!!.left
            }

        val direction: Direction?
            get() {
                if (this.parent == null) {
                    return null
                }
                return if (this === this.parent!!.left) MerklePath.Direction.LEFT else MerklePath.Direction.RIGHT
            }

        constructor(offset: Long, account: AccountInformation, allotment: Long) {
            this.offset = offset
            this.account = account
            this.allotment = allotment
        }

        @JvmOverloads
        constructor(offset: Long = 0, node: NodeInformation = NodeInformation.EMPTY_NODE, allotment: Long = 0) {
            this.offset = offset
            this.information = node
            this.allotment = allotment
        }

        constructor(prev: AugmentedMerkleTreeNode?, account: AccountInformation) {
            this.offset = if (prev == null) 0 else prev.offset + prev.allotment
            this.account = account
            this.allotment = account.allotment
        }

        constructor(proto: ProtoAugmentedMerkleTreeNode) {
            this.unmarshalProto(proto)
        }

        @JvmOverloads
        constructor(left: AugmentedMerkleTreeNode, right: AugmentedMerkleTreeNode? = null) {
            var right = right
            if (right == null) {
                right = AugmentedMerkleTreeNode(
                    left.offset + left.allotment, AccountInformation.EMPTY_ACCOUNT, 0
                )
            }
            this.left = left
            this.right = right
            //TODO
            this.left!!.parent = this
            this.right!!.parent = this
            this.offset = left.offset
            this.allotment = left.allotment + right.allotment
            this.information = NodeInformation(left.hash(), right.offset, right.hash())
        }

        override fun hash(): Hash {
            //TODO
            if (this.hash != null) {
                return this.hash!!
            }
            this.hash = Hash.of(this.toProto().toByteArray())
            return this.hash!!
        }

        override fun marshalProto(): ProtoAugmentedMerkleTreeNode {
            val builder = ProtoAugmentedMerkleTreeNode.newBuilder().setOffset(this.offset)
            if (this.account != null) {
                builder.account = this.account!!.toProto()
            }
            if (this.information != null) {
                builder.node = this.information!!.toProto()
            }
            return builder.setAllotment(this.allotment).build()
        }

        override fun unmarshalProto(proto: ProtoAugmentedMerkleTreeNode) {
            this.offset = proto.offset
            this.account = if (proto.hasAccount())
                AccountInformation.generateAccountInformation(proto.account)
            else
                null
            this.information = if (proto.hasNode()) NodeInformation.generateNodeInformation(proto.node) else null
            this.allotment = proto.allotment
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is AugmentedMerkleTreeNode) {
                return false
            }
            val treeNode = o as AugmentedMerkleTreeNode?
            return (offset == treeNode!!.offset
                    && allotment == treeNode.allotment
                    && information == treeNode.information
                    && account == treeNode.account)
        }

        override fun hashCode(): Int {
            return Objects.hash(offset, information, account, allotment)
        }

        override fun toString(): String {
            return this.toJson()
        }

        companion object {

            fun generateTreeNode(proto: ProtoAugmentedMerkleTreeNode): AugmentedMerkleTreeNode {
                val treeNode = AugmentedMerkleTreeNode()
                treeNode.unmarshalProto(proto)
                return treeNode
            }
        }
    }

    constructor(eon: Int) {
        this.eon = eon
        this.root = AugmentedMerkleTreeNode(0, AccountInformation.EMPTY_ACCOUNT, 0)
    }

    constructor(eon: Int, accountInformations: List<AccountInformation>) {
        this.eon = eon
        this.root = buildRoot(buildTreeNodes(accountInformations))
    }

    fun hash(): Hash {
        return this.root!!.hash()
    }

    fun findTreeNode(nodeHash: Hash): Optional<AugmentedMerkleTreeNode> {
        return findTreeNode(this.root) { node -> node.hash() == nodeHash }
    }

    fun findAccountInfomation(blockAddress: Address): Optional<AccountInformation> {
        val treeNode = this.findLeafNode(blockAddress)
        return if (treeNode.isPresent) {
            Optional.of(treeNode.get().account!!)
        } else {
            Optional.empty()
        }
    }

    fun findLeafNode(blockAddress: Address): Optional<AugmentedMerkleTreeNode> {
        return this.findLeafNode(Hash.of(blockAddress.toBytes()))
    }

    private fun findLeafNode(blockAddressHash: Hash?): Optional<AugmentedMerkleTreeNode> {
        return findTreeNode(
            this.root
        ) { node -> node.isLeafNode && node.account!!.address == blockAddressHash }
    }

    private fun findTreeNode(
        node: AugmentedMerkleTreeNode?, predicate: (AugmentedMerkleTreeNode) -> Boolean
    ): Optional<AugmentedMerkleTreeNode> {
        if (node == null) {
            return Optional.empty()
        }
        if (predicate(node)) {
            return Optional.of(node)
        }
        val found = findTreeNode(node.left, predicate)
        return if (found.isPresent) {
            found
        } else {
            findTreeNode(node.right, predicate)
        }
    }

    fun getMembershipProof(blockAddressHash: Hash?): AugmentedMerklePath? {
        val nodeOptional = this.findLeafNode(blockAddressHash)
        if (!nodeOptional.isPresent) {
            return null
        }
        val node = nodeOptional.get()
        val path = AugmentedMerklePath(this.eon)
        path.append(node, node.direction!!)
        var siblingNode = node.sibling
        path.append(siblingNode!!, siblingNode.direction!!)

        var parent = node.parent
        while (parent!!.parent != null) {
            siblingNode = parent.sibling
            path.append(siblingNode!!, siblingNode.direction!!)
            parent = parent.parent
        }
        return path
    }

    fun getMembershipProof(blockAddress: Address): AugmentedMerklePath {
        //TODO !!
        return this.getMembershipProof(Hash.of(blockAddress.toBytes()))!!
    }

    fun randomLeafNode(): AugmentedMerkleTreeNode {
        var node = randomChild(this.root!!)
        while (!node!!.isLeafNode) {
            node = randomChild(node)
        }
        return node
    }

    private fun randomChild(node: AugmentedMerkleTreeNode): AugmentedMerkleTreeNode? {
        if (node.isLeafNode) {
            return node
        }
        return if (RandomUtils.nextBoolean()) {
            node.left
        } else {
            node.right
        }
    }

    companion object {

        private fun buildTreeNodes(
            accountInformationList: List<AccountInformation>
        ): List<AugmentedMerkleTreeNode> {
            val prev = arrayOfNulls<AugmentedMerkleTreeNode>(1)
            return accountInformationList
                .stream()
                .map { accountInformation ->
                    val node = AugmentedMerkleTreeNode(prev[0], accountInformation)
                    prev[0] = node
                    node
                }
                .collect(Collectors.toList())
        }

        private fun buildRoot(leaves: List<AugmentedMerkleTreeNode>): AugmentedMerkleTreeNode {
            if (leaves.isEmpty()) {
                return AugmentedMerkleTreeNode()
            }
            val mergedLeaves = ArrayList<AugmentedMerkleTreeNode>()
            var i = 0
            val n = leaves.size
            while (i < n) {
                if (i < n - 1) {
                    mergedLeaves.add(AugmentedMerkleTreeNode(leaves[i], leaves[i + 1]))
                    i++
                } else {
                    mergedLeaves.add(AugmentedMerkleTreeNode(leaves[i]))
                }
                i++
            }

            return if (mergedLeaves.size > 1) {
                buildRoot(mergedLeaves)
            } else {
                mergedLeaves[0]
            }
        }

        fun buildRoot(path: AugmentedMerklePath): AugmentedMerkleTreeNode {
            val nodes = path.getNodes()

            var node: AugmentedMerkleTreeNode? = nodes!![0].node

            for (i in 1 until nodes.size) {
                val pathNode = nodes[i]
                if (pathNode.direction == MerklePath.Direction.LEFT) {
                    node = AugmentedMerkleTreeNode(pathNode.node!!, node)
                } else {
                    node = AugmentedMerkleTreeNode(node!!, pathNode.node)
                }
            }
            return node!!
        }

        fun verifyMembershipProof(
            root: AugmentedMerkleTreeNode, path: AugmentedMerklePath
        ): Boolean {
            val rootBuild = buildRoot(path)
            return rootBuild.hash() == root.hash()
        }

        fun random(count: Int): AugmentedMerkleTree {
            val eon = 1
            val accountInformationList = ArrayList<AccountInformation>()

            for (i in 0 until count) {
                val allotment = RandomUtils.nextInt(0, 10000)
                val receive = RandomUtils.nextInt(0, 10000)
                val send = RandomUtils.nextInt(0, allotment + receive)
                val a = AccountInformation(
                    Address.random(),
                    allotment.toLong(),
                    Update(eon, 0, send.toLong(), receive.toLong(), Hash.random())
                )
                accountInformationList.add(a)
            }

            return AugmentedMerkleTree(eon, accountInformationList)
        }
    }
}
