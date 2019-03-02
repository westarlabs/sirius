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

    override fun updateBatch(rows: Map<K, V>) {
        this.dataStore.updateBatch(rows.map { Pair(keyCodec.encode(it.key), valueCodec.encode(it.value)) }.toMap())
    }

    override fun keys(): List<K> {
        return this.dataStore.keys().map { keyCodec.decode(it) }
    }

    override fun forEach(consumer: (K, V) -> Unit) {
        return this.dataStore.forEach { k, v ->
            consumer(keyCodec.decode(k), valueCodec.decode(v))
        }
    }

    override fun destroy() {
        this.dataStore.destroy()
    }

    override fun init() {
        this.dataStore.init()
    }
}