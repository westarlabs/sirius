package org.starcoin.sirius.core

import java.util.stream.Collectors

abstract class Block<T : ChainTransaction> : CachedHashable() {

    abstract val height: Long

    abstract val transactions: MutableList<T>

    override fun doHash(): Hash {
        return this.blockHash()
    }

    abstract fun blockHash(): Hash

    fun addTransaction(tx: T) {
        this.transactions.add(tx)
    }


    fun filterTxByTo(to: Address): List<T> {
        return this.transactions
            .stream()
            .filter { it.to == to }
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
