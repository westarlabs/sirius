package org.starcoin.sirius.datastore

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.lang.toHEXString
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates
import kotlin.random.Random

abstract class DataStoreTestBase {

    var store: DataStore<ByteArray, ByteArray> by Delegates.notNull()

    @Before
    fun before() {
        store = createStore()
        store.init()
    }

    abstract fun createStore(): DataStore<ByteArray, ByteArray>

    @Test
    fun testPutGet() {
        val key = Random.nextBytes(10)
        val value = Random.nextBytes(10)
        store.put(key, value)
        Assert.assertArrayEquals(value, store.get(key))
        val key2 = key.copyOf()
        Assert.assertArrayEquals(value, store.get(key2))
    }

    @Test
    fun testDelete() {
        val keys = 1.rangeTo(100).map { Random.nextBytes(10) }
        keys.forEach { store.put(it, it) }
        val key = keys.random()
        Assert.assertArrayEquals(key, store.get(key))
        store.delete(key)
        Assert.assertNull(store.get(key))
        val keys2 = store.keys()
        Assert.assertEquals(keys.size - 1, keys2.size)
        keys2.forEachIndexed { index, key ->
            val key2 = keys2[index]
            Assert.assertArrayEquals("index $index ${key.toHEXString()} ${key2.toHEXString()}", key, key2)
        }
    }

    @Test
    fun testKeys() {
        val keys = 1.rangeTo(100).map { Random.nextBytes(10) }
        keys.forEach { store.put(it, it) }
        val keys2 = store.keys()
        keys.forEachIndexed { index, key ->
            Assert.assertArrayEquals(key, keys2[index])
        }
    }


    @Test
    fun testForEach() {
        val keys = 1.rangeTo(1000).map { Random.nextBytes(10) }
        keys.forEach { store.put(it, it) }
        val index = AtomicInteger(0)
        store.forEach { key, value ->
            val idx = index.getAndIncrement()
            Assert.assertArrayEquals(keys[idx], key)
        }
    }

    @After
    fun after() {
        store.destroy()
    }
}

class MapStoreTest : DataStoreTestBase() {

    override fun createStore(): DataStore<ByteArray, ByteArray> {
        return MapStore()
    }

}

class H2DBStoreTest : DataStoreTestBase() {
    override fun createStore(): DataStore<ByteArray, ByteArray> {
        return H2DBStore("test")
    }
}