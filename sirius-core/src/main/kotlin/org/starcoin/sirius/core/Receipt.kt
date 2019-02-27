package org.starcoin.sirius.core


abstract class Receipt {
    abstract val transactionHash: Hash
    abstract val status: Boolean
    abstract val logs: List<Any>?
}
