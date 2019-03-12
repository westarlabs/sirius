package org.starcoin.sirius.lang

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TestObject(load: () -> String?) {
    var count = AtomicInteger()
    var lazyString: String? by settableLazy { count.incrementAndGet(); load() }
}

class SettableLazyTest {

    @Test
    fun testSettableLazySetFirst() {
        val obj = TestObject { "world" }
        obj.lazyString = "hello"
        val str = obj.lazyString
        Assert.assertEquals("hello", str)
        Assert.assertEquals(0, obj.count.get())
    }

    @Test
    fun testSettableLazyGetAndSet() {
        val obj = TestObject { "world" }

        val str = obj.lazyString
        Assert.assertEquals("world", str)
        Assert.assertEquals(1, obj.count.get())

        obj.lazyString = "hello"
        Assert.assertEquals("hello", obj.lazyString)
        Assert.assertEquals(1, obj.count.get())
    }

    @Test
    fun testSetNull() {
        val obj = TestObject { "world" }
        obj.lazyString = null
        Assert.assertNull(obj.lazyString)
        Assert.assertEquals(0, obj.count.get())
    }

    @Test
    fun testLoadNull() {
        val obj = TestObject { null }
        Assert.assertNull(obj.lazyString)
        Assert.assertEquals(1, obj.count.get())
        Assert.assertNull(obj.lazyString)
        Assert.assertEquals(1, obj.count.get())
    }
}