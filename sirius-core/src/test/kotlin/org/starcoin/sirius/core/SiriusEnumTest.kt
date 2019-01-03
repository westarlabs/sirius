package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.reflections.Reflections
import org.starcoin.sirius.util.WithLogging
import kotlin.reflect.KClass

class SiriusEnumTest {

    companion object : WithLogging() {
        fun testClass(clazz: KClass<SiriusEnum<*>>) {
            Assert.assertTrue(clazz.java.isEnum)
            val values = clazz.java.enumConstants
            for (value in values) {
                val enumValue = value as Enum<*>
                Assert.assertEquals(enumValue.ordinal, value.number)
                Assert.assertEquals(values.size, value.toProto().descriptorForType.values.size)
            }
        }
    }

    val siriusEnumClass: MutableList<KClass<SiriusEnum<*>>> = mutableListOf()

    @Before
    fun setup() {
        val reflections = Reflections("org.starcoin.sirius.core")
        reflections.getSubTypesOf(SiriusEnum::class.java).forEach {
            LOG.info("find SiriusEnum class : ${it.name}")
            siriusEnumClass.add(it.kotlin as KClass<SiriusEnum<*>>)
        }
    }

    @Test
    fun test() {
        siriusEnumClass.forEach {
            testClass(it)
        }
    }
}
