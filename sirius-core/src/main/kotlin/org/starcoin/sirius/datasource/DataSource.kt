package org.starcoin.sirius.datasource

interface DataSource<K, V> {

    fun put(key: K, `val`: V)

    fun get(key: K): V

    fun delete(key: K)

    fun flush(): Boolean

    fun updateBatch(rows: Map<K, V>)

    fun prefixLookup(key: ByteArray, prefixBytes: Int): V

    fun keys():List<K>

    fun close()

    fun init()
}