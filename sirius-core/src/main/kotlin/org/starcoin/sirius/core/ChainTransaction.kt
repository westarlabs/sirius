package org.starcoin.sirius.core

abstract class ChainTransaction(val to: Address, val amount: Long) :
    CachedHashable() {

    abstract val from: Address?

    override fun doHash(): Hash {
        return this.txHash()
    }

    abstract fun txHash(): Hash
}
