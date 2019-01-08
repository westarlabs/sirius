package org.starcoin.sirius.core

import org.starcoin.sirius.protocol.ContractFunction
import java.math.BigInteger

abstract class ChainTransaction :
    CachedHashable() {

    abstract val from: Address?
    abstract val to: Address?
    abstract val amount: BigInteger

    abstract val isContractCall: Boolean

    abstract val contractFunction: ContractFunction<out SiriusObject>?

    override fun doHash(): Hash {
        return this.txHash()
    }

    abstract fun txHash(): Hash
}
