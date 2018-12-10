package org.starcoin.sirius.core

import com.google.protobuf.Any
import com.google.protobuf.GeneratedMessageV3
import org.apache.commons.lang3.RandomUtils
import org.starcoin.sirius.core.MerklePath.Direction
import org.starcoin.sirius.core.MerkleTree.MerkleTreeData
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoMerkleTreeNode

import java.util.*
import java.util.stream.Collectors

class MerkleTree<D : MerkleTreeData<*>> : Hashable {

    private var root: MerkleTreeNode<*>? = null

    interface MerkleTreeData<P : GeneratedMessageV3> : Hashable, ProtobufCodec<P> {
        companion object {

            val PROTO_OUT_CLASS_NAME = Starcoin::class.java.simpleName

            val implementsMap: MutableMap<Class<out GeneratedMessageV3>, Class<out MerkleTreeData<*>>> = HashMap()

            fun registerImplement(
                implementClass: Class<out MerkleTreeData<*>>,
                protobufClass: Class<out GeneratedMessageV3>
            ) {
                implementsMap[protobufClass] = implementClass
            }

            fun <D : MerkleTreeData<*>> unpark(any: Any): D? {
                if (any.value.isEmpty || any.typeUrl.isEmpty()) {
                    return null
                }
                try {
                    //example type.googleapis.com/org.starcoin.proto.ProtoHubTransaction
                    val clazzName = any.typeUrl.substring(any.typeUrl.lastIndexOf('.') + 1)
                    val clazzFullname = String
                        .format(
                            "%s.%s$%s", Starcoin.getDescriptor().getPackage(), PROTO_OUT_CLASS_NAME,
                            clazzName
                        )
                    val clazz = Class.forName(clazzFullname) as Class<GeneratedMessageV3>
                    val message = any.unpack(clazz)
                    val dataClass = implementsMap[clazz]
                    return dataClass!!.getConstructor(clazz).newInstance(message) as D
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

            }
        }
    }

    class MerkleTreeNode<D : MerkleTreeData<*>> : ProtobufCodec<ProtoMerkleTreeNode>, Hashable {

        private var hash: Hash? = null
        var data: D? = null
            private set
        @Transient
        var parent: MerkleTreeNode<*>? = null
            private set
        @Transient
        var left: MerkleTreeNode<*>? = null
        @Transient
        var right: MerkleTreeNode<*>? = null

        val sibling: MerkleTreeNode<*>?
            get() {
                if (this.parent == null) {
                    return null
                }
                return if (this === this.parent!!.left)
                    this.parent!!.right
                else
                    this.parent!!.left
            }

        val direction: Direction?
            get() {
                if (this.parent == null) {
                    return null
                }
                return if (this === this.parent!!.left)
                    MerklePath.Direction.LEFT
                else
                    MerklePath.Direction.RIGHT
            }

        internal val isLeafNode: Boolean
            get() = this.data != null

        constructor(node: ProtoMerkleTreeNode) {
            this.unmarshalProto(node)
        }

        constructor(data: D) {
            this.data = data
        }

        constructor(left: MerkleTreeNode<*>, right: MerkleTreeNode<*>?) {
            this.left = left
            this.right = right ?: left
            this.left!!.parent = this
            this.right!!.parent = this
        }

        override fun hash(): Hash {
            //TODO !!
            if (this.hash == null) {
                if (this.data != null) {
                    this.hash = this.data!!.hash()
                } else {
                    this.hash = Hash.combine(left!!.hash(), right?.hash())
                }
            }
            return hash!!
        }

        internal fun randomChild(): MerkleTreeNode<D>? {
            if (this.isLeafNode) {
                return this
            }
            return if (RandomUtils.nextBoolean()) {
                (this.left as MerkleTreeNode<D>?)
            } else {
                (this.right as MerkleTreeNode<D>?)
            }
        }

        override fun marshalProto(): ProtoMerkleTreeNode {
            val builder = ProtoMerkleTreeNode.newBuilder()
            if (this.data != null) {
                builder.data = Any.pack(this.data!!.marshalProto())
            } else {
                builder.hash = this.hash().toProto()
            }
            return builder.build()
        }

        override fun unmarshalProto(proto: ProtoMerkleTreeNode) {
            if (!proto.data.value.isEmpty) {
                this.data = MerkleTreeData.unpark(proto.data)
            } else {
                this.hash = Hash.wrap(proto.hash)
            }
        }

        override fun equals(o: kotlin.Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is MerkleTreeNode<*>) {
                return false
            }
            val that = o as MerkleTreeNode<*>?
            //if node is leaf node, just compare data.
            return if (this.data != null) {
                data == that!!.data
            } else {
                this.hash() == that!!.hash()
            }
        }

        override fun hashCode(): Int {
            return if (this.data != null) {
                Objects.hash(data)
            } else {
                Objects.hash(this.hash())
            }
        }
    }

    constructor(leaves: List<D>) {
        this.root = this.buildRoot(buildTreeNodes(leaves))
    }

    constructor(path: MerklePath<D>) {
        this.root = this.buildRoot(path)
    }

    fun getRoot(): MerkleTreeNode<D>? {
        return this.root as MerkleTreeNode<D>?
    }

    override fun hash(): Hash {
        return root!!.hash()
    }

    fun randomLeafNode(): MerkleTreeNode<D> {
        var node: MerkleTreeNode<*>? = this.root!!.randomChild()
        while (!node!!.isLeafNode) {
            node = node.randomChild()
        }
        return node as MerkleTreeNode<D>
    }

    fun findTreeNode(nodeHash: Hash): Optional<MerkleTreeNode<*>> {
        return findTreeNode(this.root) { node -> node.hash() == nodeHash }
    }

    private fun findTreeNode(
        node: MerkleTreeNode<*>?, predicate: (MerkleTreeNode<*>) -> Boolean
    ): Optional<MerkleTreeNode<*>> {
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

    fun getMembershipProof(nodeHash: Hash): MerklePath<D>? {
        val nodeOptional = this.findTreeNode(nodeHash)
        if (!nodeOptional.isPresent) {
            return null
        }
        val node = nodeOptional.get()
        val path = MerklePath<D>()
        path.append(node as MerkleTreeNode<D>, node.direction!!)
        var siblingNode: MerkleTreeNode<*>? = node.sibling
        path.append(siblingNode as MerkleTreeNode<D>, siblingNode!!.direction!!)

        var parent: MerkleTreeNode<*>? = node.parent
        while (parent!!.parent != null) {
            siblingNode = parent.sibling
            path.append(siblingNode as MerkleTreeNode<D>, siblingNode!!.direction!!)
            parent = parent.parent
        }
        return path
    }

    private fun buildRoot(leaves: List<MerkleTreeNode<D>>): MerkleTreeNode<D> {
        val mergedLeaves = ArrayList<MerkleTreeNode<D>>()
        var i = 0
        val n = leaves.size
        while (i < n) {
            if (i < n - 1) {
                mergedLeaves.add(MerkleTreeNode(leaves[i], leaves[i + 1]))
                i++
            } else {
                mergedLeaves.add(MerkleTreeNode(leaves[i], null))
            }
            i++
        }

        return if (mergedLeaves.size > 1) {
            buildRoot(mergedLeaves)
        } else {
            mergedLeaves[0]
        }
    }

    private fun buildTreeNodes(datas: List<D>): List<MerkleTreeNode<D>> {
        return datas
            .stream()
            .map { data ->
                val node = MerkleTreeNode(data)
                node
            }
            .collect(Collectors.toList())
    }

    private fun buildRoot(path: MerklePath<D>): MerkleTreeNode<D> {
        var node = path.nodes?.get(0)!!.node

        for (i in 1 until path.nodes!!.size) {
            val pathNode = path.nodes!![i]
            if (pathNode.direction == MerklePath.Direction.LEFT) {
                node = MerkleTreeNode(pathNode.node!!, node)
            } else {
                node = MerkleTreeNode(node!!, pathNode.node)
            }
        }
        return node!!
    }

    companion object {

        fun verifyMembershipProof(root: MerkleTreeNode<*>, path: MerklePath<*>): Boolean {
            val tree = MerkleTree(path)
            return tree.hash() == root.hash()
        }

        fun verifyMembershipProof(rootHash: Hash, path: MerklePath<*>): Boolean {
            val tree = MerkleTree(path)
            return tree.hash() == rootHash
        }
    }
}
