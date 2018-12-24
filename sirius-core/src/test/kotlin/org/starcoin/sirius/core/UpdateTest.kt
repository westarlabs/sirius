package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.util.WithLogging

class UpdateTest : SiriusObjectTestBase<Update>(Update::class) {
    companion object : WithLogging();

    @Test
    fun testSerialization() {
        val obj = Update.mock()
        val protoBytes = obj.toProtobuf()
        val obj1 = Update.parseFromProtobuf(protoBytes)
        Assert.assertEquals(obj, obj1)
        LOG.info(obj.toJSON())

        val protoMessage = Update.toProtoMessage(obj)
        Assert.assertEquals(obj, Update.parseFromProtoMessage(protoMessage))

        Assert.assertArrayEquals(protoBytes, protoMessage.toByteArray())
    }
}
