package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test


class NodeInformationTest : SiriusObjectTestBase<NodeInformation>(NodeInformation::class) {

    @Test
    fun testInformationProtobuf() {
        val nodeInfo = NodeInformation.mock()
        val bytes = nodeInfo.toProtobuf()
        val nodeInfo2 = NodeInformation.parseFromProtobuf(bytes)
        Assert.assertEquals(nodeInfo, nodeInfo2)
    }

    @Test
    fun testInformationProtobufMessage() {
        val nodeInfo = NodeInformation.mock()
        val protoMessage = NodeInformation.toProtoMessage(nodeInfo)

        val bytes = nodeInfo.toProtobuf()
        Assert.assertArrayEquals(bytes, protoMessage.toByteArray())

        val nodeInfo2 = NodeInformation.parseFromProtoMessage(protoMessage)
        Assert.assertEquals(nodeInfo, nodeInfo2)
    }
}
