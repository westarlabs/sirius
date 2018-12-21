package org.starcoin.sirius.crypto.eth

import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.CryptoServiceProvider

class EthCryptoServiceProvider : CryptoServiceProvider {

    override fun createService(): CryptoService {
        return EthCryptoService
    }

}
