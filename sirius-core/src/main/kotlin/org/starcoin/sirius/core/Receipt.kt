package org.starcoin.sirius.core


abstract class Receipt {
    abstract val transactionHash: Hash
    abstract val status: Boolean
    //is transaction trigger recovery mode.
    abstract val recoveryMode: Boolean
    abstract val logs: List<Any>?
}
