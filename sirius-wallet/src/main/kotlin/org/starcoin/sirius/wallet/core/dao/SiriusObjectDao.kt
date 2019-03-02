package org.starcoin.sirius.wallet.core.dao

import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.datastore.DataStore

class SiriusObjectDao<V : SiriusObject>(dataSource: DataStore<ByteArray, ByteArray>, parse: (ByteArray) -> V) :
    DataStore<Hash, V> {

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

    override fun keys(): List<Hash> {
        return dataSource.keys().map { Hash.wrap(it) }
    }

    override fun forEach(consumer: (Hash, V) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun init() = dataSource.init()


}