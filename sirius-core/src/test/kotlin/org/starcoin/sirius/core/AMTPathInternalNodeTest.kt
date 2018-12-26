package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin

class AMTPathInternalNodeTest : SiriusObjectTestBase<AMTPathInternalNode>(AMTPathInternalNode::class) {

    @Test
    fun testProtobuf() {
        val node = AMTPathInternalNode.mock()
        val bytes = node.toProtobuf()
        val node1 = AMTPathInternalNode.parseFromProtobuf(bytes)
        Assert.assertEquals(node, node1)
        val protoMessge = Starcoin.AMTPathInternalNode.parseFrom(bytes)
        val node2 = AMTPathInternalNode.parseFromProtoMessage(protoMessge)
        Assert.assertEquals(node, node2)
    }
}
