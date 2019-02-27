package org.starcoin.sirius.protocol

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt

data class TxDeferred(val txHash: Hash, val deferred: CompletableDeferred<Receipt> = CompletableDeferred()) :
    CompletableDeferred<Receipt> by deferred {

    override fun toString(): String {
        return txHash.toString()
    }

    suspend fun awaitTimoutOrNull(timeMillis: Long = 20000): Receipt? {
        return withTimeoutOrNull(timeMillis) {
            await()
        }
    }
}