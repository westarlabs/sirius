package org.starcoin.sirius.datastore

import com.google.common.io.Files

class H2DBStoreDiskTest : DataStoreTestBase() {
    override fun createStore(): DataStore<ByteArray, ByteArray> {
        val dir = Files.createTempDir()
        dir.deleteOnExit()
        return H2DBStore("test", dir)
    }
}