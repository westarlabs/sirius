package org.starcoin.sirius.datastore

interface DataStore<K, V> {

    fun put(key: K, value: V)

    fun get(key: K): V?

    fun delete(key: K)

    fun flush(): Boolean

    fun updateBatch(rows: Map<K, V>)

    /**
     * should keep insert order.
     */
    fun keys(): List<K>

    /**
     * should keep insert order.
     */
    fun forEach(consumer: (K, V) -> Unit)

    fun destroy()

    fun init()
}