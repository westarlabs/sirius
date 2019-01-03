package org.starcoin.sirius.core

import org.junit.Test
import kotlin.reflect.KClass

abstract class SiriusObjectTestBase<T : SiriusObject>(val objectClass: KClass<T>) {

    @Test
    fun test() {
        SiriusObjectSerializationTest.testClass(objectClass)
    }
}
