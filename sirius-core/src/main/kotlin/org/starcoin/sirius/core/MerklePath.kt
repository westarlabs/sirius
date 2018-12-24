package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.*
import org.starcoin.sirius.core.MerkleTree.MerkleTreeData
import org.starcoin.sirius.core.MerkleTree.MerkleTreeNode
import java.util.*
import java.util.stream.Collectors

class MerklePath<D : MerkleTreeData> : ProtobufCodec<ProtoMerklePath> {

    internal var nodes: MutableList<MerklePathNode<D>>? = null

    // TODO
    val leafNode: MerklePathNode<*>
        get() = nodes!![0]

    constructor() {
        this.nodes = ArrayList()
    }

    constructor(proto: ProtoMerklePath) {
        this.unmarshalProto(proto)
    }

    fun append(node: MerkleTreeNode<D>, direction: Direction) {
        this.nodes!!.add(MerklePathNode(node, direction))
    }

    fun getNodes(): List<MerklePathNode<D>> {
        return Collections.unmodifiableList(nodes!!)
    }

    override fun marshalProto(): ProtoMerklePath {
        return ProtoMerklePath.newBuilder()
            .addAllNodes(this.nodes!!.stream().map { it.toProto() }.collect(Collectors.toList()))
            .build()
    }

    override fun unmarshalProto(proto: ProtoMerklePath) {
        this.nodes = proto
            .nodesList
            .stream()
            .map { MerklePathNode<D>(it) }
            .collect(Collectors.toList())
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is MerklePath<*>) {
            return false
        }
        val that = o as MerklePath<*>?
        return nodes == that!!.nodes
    }

    override fun hashCode(): Int {
        return Objects.hash(nodes)
    }

    enum class Direction constructor(private val protoType: ProtoMerklePathDirection) :
        ProtoEnum<ProtoMerklePathDirection> {
        UNKNOWN(ProtoMerklePathDirection.DIRECTION_UNKNOWN),
        LEFT(ProtoMerklePathDirection.DIRECTION_LEFT),
        RIGHT(ProtoMerklePathDirection.DIRECTION_RIGTH);

        override fun toProto(): ProtoMerklePathDirection {
            return protoType
        }

        companion object {

            fun valueOf(number: Int): Direction {
                for (direction in Direction.values()) {
                    if (direction.number == number) {
                        return direction
                    }
                }
                return Direction.UNKNOWN
            }
        }
    }

    class MerklePathNode<D : MerkleTreeData> : ProtobufCodec<ProtoMerklePathNode> {

        var node: MerkleTreeNode<D>? = null
            private set
        // TODO ensure type, use a flag?
        var direction: Direction? = null
            private set

        constructor() {}

        constructor(proto: ProtoMerklePathNode) {
            this.unmarshalProto(proto)
        }

        constructor(node: MerkleTreeNode<D>, direction: Direction) {
            this.node = node
            this.direction = direction
        }

        override fun marshalProto(): ProtoMerklePathNode {
            return ProtoMerklePathNode.newBuilder()
                .setNode(node!!.toProto())
                .setDirection(this.direction!!.toProto())
                .build()
        }

        override fun unmarshalProto(proto: ProtoMerklePathNode) {
            this.node = MerkleTreeNode(proto.node)
            this.direction = Direction.valueOf(proto.direction.number)
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is MerklePathNode<*>) {
                return false
            }
            val that = o as MerklePathNode<*>?
            return node == that!!.node && direction == that.direction
        }

        override fun hashCode(): Int {
            return Objects.hash(node, direction)
        }


        override fun toString(): String {
            return this.toJson()
        }

    }

    companion object {

        fun <D : MerkleTreeData> generateMerklePath(proto: ProtoMerklePath): MerklePath<*> {
            val path = MerklePath<D>()
            path.unmarshalProto(proto)
            return path
        }
    }
}
