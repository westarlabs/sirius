package org.starcoin.sirius.core

import com.google.protobuf.GeneratedMessageV3
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.reflections.Reflections
import org.starcoin.sirius.util.WithLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName


open class SiriusObjectSerializationTest {

    companion object : WithLogging() {

        fun <T : SiriusObject> testClass(clazz: KClass<T>) {
            LOG.info("test ${clazz.jvmName}")
            if (clazz.companionObjectInstance !is SiriusObjectCompanion<*, *>) {
                LOG.warning("Can not find SiriusObjectCompanion for class $clazz")
                return
            }
            val companion = clazz.companionObjectInstance as SiriusObjectCompanion<T, GeneratedMessageV3>
            val obj = companion.mock()

            LOG.info("test ${clazz.jvmName} JSON")
            val json = companion.toJSON(obj)
            LOG.info(json)
            val obj4 = companion.parseFromJSON(json)
            Assert.assertEquals(obj, obj4)


            LOG.info("test ${clazz.jvmName} Protobuf")
            val protoMessage = companion.toProtoMessage(obj)
            val protobufBytes = companion.toProtobuf(obj)

            val obj1 = companion.parseFromProtobuf(protobufBytes)
            val obj2 = companion.parseFromProtoMessage(protoMessage)
            Assert.assertEquals(obj, obj1)
            Assert.assertEquals(obj, obj2)
            Assert.assertArrayEquals(protoMessage.toByteArray(), protobufBytes)

            LOG.info("test ${clazz.jvmName} RLP")
            val rlpBytes = companion.toRLP(obj)
            val obj3 = companion.parseFromRLP(rlpBytes)
            Assert.assertEquals(obj, obj3)
        }
    }

    val siriusObjectClass: MutableList<KClass<out SiriusObject>> = mutableListOf()

    @Before
    fun setup() {
        val reflections = Reflections("org.starcoin.sirius.core")
        reflections.getSubTypesOf(SiriusObject::class.java).filter { !it.kotlin.isAbstract }.forEach {
            LOG.info("find SiriusObject class : ${it.name}")
            siriusObjectClass.add(it.kotlin as KClass<out SiriusObject>)
        }
    }

    @Test
    fun test() {
        siriusObjectClass.forEach {
            testClass(it)
        }
    }

    @Test
    fun testSignature() {
        Assert.assertTrue(Signature.ZERO_SIGN.isZero())
        Assert.assertFalse(Signature.random().isZero())
    }
}
