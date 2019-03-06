package org.starcoin.sirius.datastore

import org.starcoin.sirius.lang.toHEXString

private class ByteArrayWrapper(val bytes: ByteArray) {

    val size: Int
        get() = bytes.size

    override fun toString(): String {
        return bytes.toHEXString()
    }

    fun toBytes() = this.bytes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayWrapper) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

class MapStore : DataStore<ByteArray, ByteArray> {

    // ByteArray can not as map's key, because ByteArray's hash is suitable for map
    private val map = HashMap<ByteArrayWrapper, ByteArray>()
    private val keyList = mutableListOf<ByteArrayWrapper>()

    override fun put(key: ByteArray, value: ByteArray) {
        val keyWrapper = ByteArrayWrapper(key)
        val preValue = map.put(keyWrapper, value)
        //if preValue not exist, insert new key to list.
        if (preValue == null) {
            keyList.add(keyWrapper)
        }
    }

    override fun get(key: ByteArray): ByteArray? {
        return this.doGet(ByteArrayWrapper(key))
    }

    private fun doGet(key: ByteArrayWrapper): ByteArray? {
        return map[key]
    }

    @Synchronized
    override fun delete(key: ByteArray) {
        val keyWrapper = ByteArrayWrapper(key)
        val value = map.remove(keyWrapper)
        value?.let { keyList.remove(keyWrapper) }
    }

    override fun flush(): Boolean {
        return true
    }

    @Synchronized
    override fun updateBatch(rows: List<Pair<ByteArray, ByteArray>>) {
        for ((k, v) in rows) {
            this.put(k, v)
        }
    }

    @Synchronized
    override fun keys(): List<ByteArray> {
        return keyList.map { it.bytes }
    }

    @Synchronized
    override fun forEach(consumer: (ByteArray, ByteArray) -> Unit) {
        this.keyList.forEach { key ->
            val value = doGet(key)!!
            consumer(key.bytes, value)
        }
    }

    override fun iterator(): CloseableIterator<Pair<ByteArray, ByteArray>> {
        return CloseableIterator(this.keyList.iterator().asSequence().map { key ->
            val value = doGet(key)!!
            Pair(key.bytes, value)
        }.iterator())
    }

    override fun destroy() {
        this.map.clear()
        this.keyList.clear()
    }

    override fun init() {
    }

}