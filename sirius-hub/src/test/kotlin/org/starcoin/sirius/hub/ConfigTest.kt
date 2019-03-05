package org.starcoin.sirius.hub

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.Address

class ConfigTest {

    @Test
    fun testConfigStore() {
        val config = Config.configurationForUNIT()
        config.contractAddress = Address.DUMMY_ADDRESS
        config.store()
        val config2 = Config.loadConfig(config.dataDir)
        Assert.assertEquals(config.contractAddress, config2.contractAddress)
        Assert.assertEquals(config.properties, config2.properties)
    }
}