package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin

class AMTreePathInternalNodeTest : SiriusObjectTestBase<AMTreePathInternalNode>(AMTreePathInternalNode::class) {

    @Test
    fun testProtobuf() {
        val node = AMTreePathInternalNode.mock()
        val bytes = node.toProtobuf()
        val node1 = AMTreePathInternalNode.parseFromProtobuf(bytes)
        Assert.assertEquals(node, node1)
        val protoMessge = Starcoin.AMTreePathInternalNode.parseFrom(bytes)
        val node2 = AMTreePathInternalNode.parseFromProtoMessage(protoMessge)
        Assert.assertEquals(node, node2)
    }
}
