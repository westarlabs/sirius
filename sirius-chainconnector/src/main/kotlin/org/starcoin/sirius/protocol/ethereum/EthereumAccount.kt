package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.util.WithLogging
import java.util.concurrent.atomic.AtomicLong

class EthereumAccount(
    override val key: CryptoKey = CryptoService.generateCryptoKey(),
    private val nonce: AtomicLong = AtomicLong(0)
) : ChainAccount() {

    companion object : WithLogging() {
        val DUMMY_ACCOUNT = EthereumAccount(CryptoService.dummyCryptoKey)
    }

    fun getNonce(): Long = this.nonce.get()

    fun incAndGetNonce(): Long {
        val value = this.nonce.incrementAndGet()
        LOG.fine("$address nonce $value")
        return value
    }
}
