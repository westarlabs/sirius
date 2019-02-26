package org.starcoin.sirius.wallet.core.dao

import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.datasource.DataSource

class SiriusObjectDao<V :SiriusObject>(dataSource: DataSource<ByteArray,ByteArray>) : DataSource<Hash,V>{

    private val dataSource = dataSource

    override fun put(key: Hash, `val`: V) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun get(key: Hash): V? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(key: Hash) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun init() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}