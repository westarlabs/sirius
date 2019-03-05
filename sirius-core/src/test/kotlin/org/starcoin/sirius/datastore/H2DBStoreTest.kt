package org.starcoin.sirius.datastore

class H2DBStoreTest : DataStoreTestBase() {
    override fun createStore(): DataStore<ByteArray, ByteArray> {
        return H2DBStore("test")
    }
}