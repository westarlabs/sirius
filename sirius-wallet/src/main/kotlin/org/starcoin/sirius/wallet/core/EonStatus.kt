package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*
import kotlin.properties.Delegates

class EonStatus() {

    var eon: Eon by Delegates.notNull()

    var path: AugmentedMerklePath? = null

    var transactionHistory: MutableList<OffchainTransaction> = mutableListOf()

    var updateHistory: MutableList<Update> = mutableListOf()

    var transactionMap: MutableMap<String, OffchainTransaction> = mutableMapOf()

    var confirmedTransactions: MutableList<ChainTransaction> = mutableListOf()

    var deposit: Long = 0

    var allotment: Long = 0

    constructor(eon: Eon, allotment: Long) : this() {
        this.eon = eon
        this.allotment = allotment
    }
}