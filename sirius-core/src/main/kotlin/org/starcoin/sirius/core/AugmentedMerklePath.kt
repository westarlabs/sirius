package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.ProtoAugmentedMerklePath
import org.starcoin.proto.Starcoin.ProtoAugmentedMerklePathNode
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode
import org.starcoin.sirius.core.MerklePath.Direction
import java.util.*
import java.util.stream.Collectors

class AugmentedMerklePath : ProtobufCodec<ProtoAugmentedMerklePath>, Hashable {

    var eon: Int = 0
        private set
    private var nodes: MutableList<AugmentedMerklePathNode>? = null
    @Transient
    private var hash: Hash? = null

    val leaf: AugmentedMerkleTreeNode?
        get() = this.getNodes()!![0].node

    constructor() {}

    class AugmentedMerklePathNode : ProtobufCodec<ProtoAugmentedMerklePathNode> {

        var node: AugmentedMerkleTreeNode? = null
            private set
        // TODO ensure type, use a flag?
        var direction: Direction? = null
            private set

        constructor() {}

        constructor(node: AugmentedMerkleTreeNode, direction: Direction) {
            this.node = node
            this.direction = direction
        }

        constructor(protoPathNode: ProtoAugmentedMerklePathNode) {
            this.unmarshalProto(protoPathNode)
        }

        override fun marshalProto(): ProtoAugmentedMerklePathNode {
            return ProtoAugmentedMerklePathNode.newBuilder()
                .setNode(this.node!!.toProto())
                .setDirection(this.direction!!.toProto())
                .build()
        }

        override fun unmarshalProto(proto: ProtoAugmentedMerklePathNode) {
            this.node = AugmentedMerkleTreeNode.generateTreeNode(proto.node)
            this.direction = MerklePath.Direction.valueOf(proto.direction.number)
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is AugmentedMerklePathNode) {
                return false
            }
            val that = o as AugmentedMerklePathNode?
            return node == that!!.node && direction == that.direction
        }

        override fun hashCode(): Int {
            return Objects.hash(node, direction)
        }
    }

    constructor(eon: Int) {
        this.eon = eon
        this.nodes = ArrayList()
    }

    constructor(protoMerklePath: ProtoAugmentedMerklePath) {
        this.unmarshalProto(protoMerklePath)
    }

    fun append(node: AugmentedMerkleTreeNode, direction: Direction) {
        this.nodes!!.add(AugmentedMerklePathNode(node, direction))
        this.hash = null
    }

    fun getNodes(): List<AugmentedMerklePathNode>? {
        return nodes
    }

    override fun marshalProto(): ProtoAugmentedMerklePath {
        return ProtoAugmentedMerklePath.newBuilder()
            .setEon(this.eon)
            .addAllNodes(
                this.nodes!!.stream().map<ProtoAugmentedMerklePathNode> { it.toProto() }.collect(
                    Collectors.toList()
                )
            )
            .build()
    }

    override fun unmarshalProto(proto: ProtoAugmentedMerklePath) {
        this.eon = proto.eon
        this.nodes = proto
            .nodesList
            .stream()
            .map<AugmentedMerklePathNode> { AugmentedMerklePathNode(it) }
            .collect(Collectors.toList())
    }

    override fun hash(): Hash {
        //TODO use lazy calculate get.
        if (this.hash != null) {
            return this.hash!!
        }
        this.hash = Hash.of(this.marshalProto().toByteArray())
        return this.hash!!
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is AugmentedMerklePath) {
            return false
        }
        val that = o as AugmentedMerklePath?
        return eon == that!!.eon && nodes == that.nodes
    }

    override fun hashCode(): Int {
        return Objects.hash(eon, nodes)
    }

    override fun toString(): String {
        return this.toJson()
    }
}
