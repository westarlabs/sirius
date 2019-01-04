package org.starcoin.sirius.core

import java.math.BigInteger

abstract class ChainTransaction(val to: Address, val amount: BigInteger) :
    CachedHashable() {

    abstract val from: Address?

    override fun doHash(): Hash {
        return this.txHash()
    }

    abstract fun txHash(): Hash
}
