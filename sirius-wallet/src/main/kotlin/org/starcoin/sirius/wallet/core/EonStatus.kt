package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*
import java.math.BigInteger
import kotlin.properties.Delegates

class EonStatus() {

    var eon: Eon by Delegates.notNull()

    var treeProof: AMTreeProof? = null

    var transactionHistory: MutableList<OffchainTransaction> = mutableListOf()

    var updateHistory: MutableList<Update> = mutableListOf()

    var transactionMap: MutableMap<String, OffchainTransaction> = mutableMapOf()

    var confirmedTransactions: MutableList<ChainTransaction> = mutableListOf()

    var deposit: Long = 0

    var allotment: BigInteger = BigInteger.ZERO

    constructor(eon: Eon, allotment: BigInteger) : this() {
        this.eon = eon
        this.allotment = allotment
    }
}
