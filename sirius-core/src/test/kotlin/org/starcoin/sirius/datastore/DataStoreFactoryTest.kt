package org.starcoin.sirius.datastore

import com.google.common.io.Files
import org.junit.Assert
import org.junit.Test

class DataStoreFactoryTest {

    @Test
    fun testH2DBDataStoreFactory() {
        val dataFactory = H2DBDataStoreFactory(Files.createTempDir().apply { deleteOnExit() })
        val dataStore = dataFactory.getOrCreate("test")
        Assert.assertTrue(dataFactory.exist("test"))
        dataStore.destroy()
        Assert.assertFalse(dataFactory.exist("test"))
    }
}