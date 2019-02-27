package org.starcoin.sirius.wallet.core.dao

import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.datasource.DataSource

class SiriusObjectDao<V :SiriusObject>(dataSource: DataSource<ByteArray,ByteArray>,parse:(ByteArray)->V) : DataSource<Hash,V>{

    private val dataSource = dataSource

    private val parse = parse

    override fun put(key: Hash, value: V) = dataSource.put(key.toBytes(),value.toProtobuf())

    override fun get(key: Hash): V? {
        val byteArray=dataSource.get(key.toBytes())
        return byteArray?.run { parse(byteArray) }
    }

    override fun delete(key: Hash) = dataSource.delete(key.toBytes())

    override fun flush(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateBatch(rows: Map<Hash, V>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prefixLookup(key: ByteArray, prefixBytes: Int): V {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keys(): Set<Hash> {
        return dataSource.keys().map { Hash.wrap(it) }.toSet()
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun init() = dataSource.init()


}