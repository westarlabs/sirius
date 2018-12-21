package org.starcoin.sirius.crypto.fallback;

import org.junit.Assert
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoServiceTestBase

class FallbackCryptoServiceTest : CryptoServiceTestBase() {
    override fun assertCryptoServiceType(service: CryptoService) {
        Assert.assertTrue(service is FallbackCryptoService)
    }
}
