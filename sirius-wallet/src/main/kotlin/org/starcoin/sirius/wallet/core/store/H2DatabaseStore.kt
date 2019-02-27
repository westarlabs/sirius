package org.starcoin.sirius.wallet.core.store

import org.sql2o.Sql2o
import org.starcoin.sirius.datasource.DataSource

class H2DatabaseStore(sql2o: Sql2o, tableName:String):DataSource<ByteArray,ByteArray>{

    private val sql2o= sql2o

    private val tableName = tableName

    override fun put(key: ByteArray, value: ByteArray) {
        sql2o.beginTransaction().use { con ->
            con.createQuery("insert into $tableName values(:key,:value)")
                .addParameter("key", key)
                .addParameter("value", value)
                .executeUpdate()
            con.commit()
        }
    }

    override fun get(key: ByteArray): ByteArray? {
        sql2o.open().use { con ->
            val query = "SELECT value FROM $tableName WHERE key = :key"

            return con.createQuery(query)
                .addParameter("key", key).executeAndFetchFirst(ByteArray::class.java)
        }
    }

    override fun delete(key: ByteArray) {
        sql2o.beginTransaction().use { con ->
            con.createQuery("delete from $tableName where key=:key")
                .addParameter("key", key)
                .executeUpdate()
            con.commit()
        }
    }

    override fun flush(): Boolean {
        return true
    }

    override fun updateBatch(rows: Map<ByteArray, ByteArray>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prefixLookup(key: ByteArray, prefixBytes: Int): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun keys(): Set<ByteArray> {
        sql2o.open().use { con ->
            val query = "SELECT key FROM $tableName"
            return con.createQuery(query).executeAndFetch(ByteArray::class.java).toHashSet()
        }
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun init() {
        sql2o.beginTransaction().use { con ->
            con.createQuery("create table IF NOT EXISTS $tableName (key BINARY(100),value BINARY(1000))")
                .executeUpdate()
            con.commit()
        }
    }

}