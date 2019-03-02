package org.starcoin.sirius.datastore

interface DataStoreFactory {

    fun create(name: String): DataStore<ByteArray, ByteArray>

    fun get(name: String): DataStore<ByteArray, ByteArray>?

    fun getOrCreate(name: String): DataStore<ByteArray, ByteArray>

    fun exist(name: String): Boolean

    fun delete(name: String)
}

class DataStoreExistException(name: String) : RuntimeException("DataStore with name $name exist")

class MapDataStoreFactory : DataStoreFactory {

    val stores = mutableMapOf<String, MapStore>()

    override fun create(name: String): MapStore {
        if (stores.containsKey(name)) {
            throw DataStoreExistException(name)
        }
        val store = MapStore()
        stores[name] = store
        return store
    }

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