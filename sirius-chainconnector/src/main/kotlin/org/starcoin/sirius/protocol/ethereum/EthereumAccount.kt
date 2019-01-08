package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ChainAccount
import java.util.concurrent.atomic.AtomicLong

class EthereumAccount(override val key: CryptoKey, private val nonce: AtomicLong = AtomicLong(0)) : ChainAccount() {

    companion object {
        val DUMMY_ACCOUNT = EthereumAccount(CryptoService.dummyCryptoKey)
    }

    fun getNonce(): Long = this.nonce.get()

    fun getAndIncNonce(): Long = this.nonce.getAndIncrement()
}
