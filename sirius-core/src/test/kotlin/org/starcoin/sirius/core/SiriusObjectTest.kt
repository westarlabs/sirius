package org.starcoin.sirius.core

import kotlinx.serialization.Serializable
import org.junit.Assert
import org.junit.Test

@Serializable
class TestObject() : SiriusObject() {
    var name: String = ""
    var age: Int = 0
        set(value) {
            field = value
            resetHash()
        }

    constructor(name: String, age: Int) : this() {
        this.name = name
        this.age = age
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestObject) return false

        if (name != other.name) return false
        if (age != other.age) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + age
        return result
    }

}

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
