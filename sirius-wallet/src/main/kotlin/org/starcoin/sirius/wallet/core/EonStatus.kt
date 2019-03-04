package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*
import java.math.BigInteger
import kotlin.properties.Delegates

class EonStatus() {

    internal var eon: Int by Delegates.notNull()

    internal var treeProof: AMTreeProof? = null

    internal var transactionHistory: MutableList<OffchainTransaction> = mutableListOf()

    internal var updateHistory: MutableList<Update> = mutableListOf()

    internal var transactionMap: MutableMap<Hash, OffchainTransaction> = mutableMapOf()

    internal var confirmedTransactions: MutableList<ChainTransaction> = mutableListOf()

    internal var deposit: BigInteger = BigInteger.ZERO

    //internal var allotment: BigInteger = BigInteger.ZERO

    constructor(eon: Int) : this() {
        this.eon = eon
        //this.allotment = allotment
    }

    internal fun addDeposit(transaction: ChainTransaction){
        this.deposit = deposit.add(transaction.amount)
        //this.allotment = allotment.add(transaction.amount)

        confirmedTransactions.add(transaction)
    }

}
