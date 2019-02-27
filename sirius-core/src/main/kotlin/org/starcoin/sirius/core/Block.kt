package org.starcoin.sirius.core

import org.starcoin.sirius.protocol.TransactionResult
import java.util.stream.Collectors

abstract class Block<T : ChainTransaction> : CachedHashable() {

    // FIXME: Use BigInbteger
    abstract val height: Long

    abstract val transactions: List<TransactionResult<T>>

    override fun doHash(): Hash {
        return this.blockHash()
    }

    abstract fun blockHash(): Hash

    fun filterTxByTo(to: Address): List<TransactionResult<T>> {
        return this.transactions
            .stream()
            .filter { it.tx.to == to }
            .collect(Collectors.toList())
    }

    override fun toString(): String {
        return "Block:{height:$height, hash:$id}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Block<*>) return false

        if (height != other.height) return false
        if (transactions != other.transactions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = height.hashCode()
        result = 31 * result + transactions.hashCode()
        return result
    }
}
