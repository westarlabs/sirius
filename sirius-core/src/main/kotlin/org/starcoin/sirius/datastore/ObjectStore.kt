package org.starcoin.sirius.datastore

import org.starcoin.sirius.serialization.Codec

open class ObjectStore<K, V>(
    private val keyCodec: Codec<K>,
    private val valueCodec: Codec<V>,
    private val dataStore: DataStore<ByteArray, ByteArray>
) : DataStore<K, V> {

    override fun put(key: K, value: V) {
        this.dataStore.put(keyCodec.encode(key), valueCodec.encode(value))
    }

    override fun get(key: K): V? {
        return this.dataStore.get(keyCodec.encode(key))?.let { valueCodec.decode(it) }
    }

    override fun delete(key: K) {
        this.dataStore.delete(keyCodec.encode(key))
    }

    override fun flush(): Boolean {
        return this.dataStore.flush()
    }

    override fun updateBatch(rows: List<Pair<K, V>>) {
        this.dataStore.updateBatch(rows.map { Pair(keyCodec.encode(it.first), valueCodec.encode(it.second)) })
    }

    override fun keys(): List<K> {
        return this.dataStore.keys().map { keyCodec.decode(it) }
    }

    override fun iterator(): CloseableIterator<Pair<K, V>> {
        return this.dataStore.iterator().map { Pair(keyCodec.decode(it.first), valueCodec.decode(it.second)) }
    }

    override fun destroy() {
        this.dataStore.destroy()
    }

    override fun init() {
        this.dataStore.init()
    }
}