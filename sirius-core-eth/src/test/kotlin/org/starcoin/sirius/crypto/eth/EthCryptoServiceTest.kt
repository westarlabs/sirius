package org.starcoin.sirius.crypto.eth

import org.junit.Assert
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoServiceTestBase

class EthCryptoServiceTest : CryptoServiceTestBase() {

    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is EthCryptoService)
    }
}
