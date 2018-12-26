package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test

class SiriusObjectTest {

    @Test
    fun testSiriusObject() {
        val obj = TestObject("test", 10)

        val json = obj.toJSON()
        println(json)
        val obj3 = SiriusObject.parseFromJSON<TestObject>(json)
        Assert.assertEquals(obj, obj3)

        val rlpBytes = obj.toRLP()
        val obj1 = SiriusObject.parseFromRLP<TestObject>(rlpBytes)
        Assert.assertEquals(obj, obj1)

        val protobufBytes = obj.toProtobuf()
        val obj2 = SiriusObject.parseFromProtobuf<TestObject>(protobufBytes)
        Assert.assertEquals(obj, obj2)
    }

    @Test
    fun testResetHash() {
        val obj = TestObject("test", 10)
        val hash = obj.hash()
        obj.name = obj.name + "1"
        val hash1 = obj.hash()
        Assert.assertEquals(hash, hash1)
        obj.age = obj.age + 1
        val hash2 = obj.hash()
        Assert.assertNotEquals(hash, hash2)
        val hash3 = obj.hash()
        Assert.assertEquals(hash2, hash3)
    }
}
