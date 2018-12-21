package org.starcoin.sirius.core

import kotlinx.serialization.Serializable
import org.junit.Assert
import org.junit.Test

@Serializable
data class TestObject(val name: String, val age: Int) : SiriusObject()

class SiriusObjectTest {

    @Test
    fun testSiriusObjectPrintln(){
        val obj = TestObject("test", 10)
        obj.printClass()
    }

    @Test
    fun testSiriusObject() {
        val obj = TestObject("test", 10)
        val rlpBytes = obj.toRLP()
        val obj1 = SiriusObject.fromRLP<TestObject>(rlpBytes)
        Assert.assertEquals(obj, obj1)

        val protobufBytes = obj.toProtobuf()
        val obj2 = SiriusObject.fromProtobuf<TestObject>(protobufBytes)
        Assert.assertEquals(obj, obj2)

        val json = obj.toJSON()
        println(json)
        val obj3 = SiriusObject.fromJSON<TestObject>(json)
        Assert.assertEquals(obj, obj3)
    }
}
