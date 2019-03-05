package org.starcoin.sirius.datastore

import java.io.File

interface DataStoreFactory {

    fun get(name: String): DataStore<ByteArray, ByteArray>?

    fun getOrCreate(name: String): DataStore<ByteArray, ByteArray>

    fun exist(name: String): Boolean

    fun delete(name: String)
}

class MapDataStoreFactory : DataStoreFactory {

    val stores = mutableMapOf<String, MapStore>()

    override fun get(name: String): MapStore? {
        return stores[name]
    }

    override fun getOrCreate(name: String): MapStore {
        return stores[name] ?: {
            val store = MapStore()
            stores[name] = store
            store
        }.invoke()
    }

    override fun exist(name: String): Boolean {
        return stores.containsKey(name)
    }

    override fun delete(name: String) {
        stores.remove(name)
    }

}

class H2DBDataStoreFactory(val dbDir: File) : DataStoreFactory {
    val defaultDB = H2DBStore("default", dbDir)

    override fun get(name: String): H2DBStore? {
        return defaultDB.getTable(name)
    }

    override fun getOrCreate(name: String): H2DBStore {
        return defaultDB.getOrCreateTable(name)
    }

    override fun exist(name: String): Boolean {
        return defaultDB.existTable(name)
    }

    override fun delete(name: String) {
        this.get(name)?.apply { destroy() }
    }
}