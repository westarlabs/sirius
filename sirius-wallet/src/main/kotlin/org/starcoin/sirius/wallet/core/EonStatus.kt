package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*
import java.util.*

class EonStatus() {

    var eon: Eon? = null

    var path: AugmentedMerklePath? = null

    var transactionHistory: List<OffchainTransaction>

    var updateHistory: List<Update>

    var transactionMap: Map<String, OffchainTransaction>

    var confirmedTransactions: List<ChainTransaction>

    var deposit: Long = 0

    var allotment: Long = 0

    init {
        this.transactionHistory = ArrayList<OffchainTransaction>()
        this.updateHistory = ArrayList<Update>()
        this.transactionMap = HashMap<String, OffchainTransaction>()
        this.confirmedTransactions = ArrayList<ChainTransaction>()
    }

    constructor(eon: Eon, allotment: Long) : this() {
        this.eon = eon
        this.allotment = allotment
    }
}