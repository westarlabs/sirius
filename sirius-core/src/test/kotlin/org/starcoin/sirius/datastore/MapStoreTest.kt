package org.starcoin.sirius.datastore

class MapStoreTest : DataStoreTestBase() {

    override fun createStore(): DataStore<ByteArray, ByteArray> {
        return MapStore()
    }

}