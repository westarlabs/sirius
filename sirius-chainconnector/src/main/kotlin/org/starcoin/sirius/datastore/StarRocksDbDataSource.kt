package org.starcoin.sirius.datastore

import org.ethereum.datasource.rocksdb.RocksDbDataSource
import org.starcoin.sirius.datasource.DataSource

class StarRocksDbDataSource(rocksDbDataSource: RocksDbDataSource) : DataSource<ByteArray,ByteArray> {

    private val rocksDbDataSource = rocksDbDataSource

    override fun put(key: ByteArray, value: ByteArray) = this.rocksDbDataSource.put(key,value)

    override fun get(key: ByteArray): ByteArray = this.rocksDbDataSource.get(key)

    override fun delete(key: ByteArray)=this.rocksDbDataSource.delete(key)


    override fun flush(): Boolean = this.rocksDbDataSource.flush()

    override fun updateBatch(rows: Map<ByteArray, ByteArray>) = this.rocksDbDataSource.updateBatch(rows)

    override fun prefixLookup(key: ByteArray, prefixBytes: Int) = this.rocksDbDataSource.prefixLookup(key,prefixBytes)

    override fun keys(): Set<ByteArray> = rocksDbDataSource.keys()
    
    override fun close() = rocksDbDataSource.close()

    override fun init() = rocksDbDataSource.init()

}