package org.starcoin.sirius.core

import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoHubRoot
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode
import java.util.*

//TODO
class HubRoot : SiriusObject {

    var node: NodeInformation? = null
        private set
    var offset: Long = 0
        private set
    var allotment: Long = 0
        private set

    var eon: Int = 0
        private set

    constructor() {}

    constructor(proto: ProtoHubRoot) {
        this.unmarshalProto(proto)
    }

    constructor(root: AugmentedMerkleTreeNode, eon: Int) {
        this.eon = eon
        this.node = root.information
        this.allotment = root.allotment
        this.offset = root.offset
    }

    fun marshalProto(): Starcoin.ProtoHubRoot {
        val builder = Starcoin.ProtoHubRoot.newBuilder()
        builder.eon = this.eon

        if (this.node != null) {
            val root = AugmentedMerkleTreeNode(this.offset, this.node!!, this.allotment)
            builder.root = root.toProto()
        }
        return builder.build()
    }

    fun unmarshalProto(proto: Starcoin.ProtoHubRoot) {
        this.eon = proto.eon

        if (proto.hasRoot()) {
            val root = AugmentedMerkleTreeNode()
            root.unmarshalProto(proto.root)
            this.offset = root.offset
            this.allotment = root.allotment
            this.node = root.information
        }
    }

    fun mock(context: MockContext) {
        this.allotment = RandomUtils.nextInt().toLong()
        this.offset = RandomUtils.nextInt().toLong()
        this.eon = RandomUtils.nextInt()
        this.node = context.getOrDefault("nodeinformation", NodeInformation())
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is HubRoot) {
            return false
        }
        val hubRoot = o as HubRoot?
        return (this.eon == hubRoot!!.eon
                && this.offset == hubRoot.offset
                && this.allotment == hubRoot.allotment
                && this.node == hubRoot.node)
    }

    override fun hashCode(): Int {
        return Objects.hash(this.node, this.offset, this.allotment, this.eon)
    }

    fun hubRoot2AugmentedMerkleTreeNode(): AugmentedMerkleTreeNode? {
        return if (this.node != null) {
            AugmentedMerkleTreeNode(this.offset, this.node!!, this.allotment)
        } else null
    }


    companion object {

        fun generateNodeInformation(proto: Starcoin.ProtoHubRoot): HubRoot {
            val hubRoot = HubRoot()
            hubRoot.unmarshalProto(proto)
            return hubRoot
        }
    }
}
