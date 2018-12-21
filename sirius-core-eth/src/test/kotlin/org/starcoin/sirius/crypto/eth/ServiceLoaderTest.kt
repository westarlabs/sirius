package org.starcoin.sirius.crypto.eth

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService

class ServiceLoaderTest {

    @Test
    fun testServiceLoader() {
        val cryptoService = CryptoService.createCryptoService()
        Assert.assertTrue(cryptoService is EthCryptoService)
    }
}
