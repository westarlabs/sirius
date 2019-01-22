package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*

data class LocalEonState(
    var previous: LocalEonState?,
    val eon: Int,
    var update: Update,
    val txs: MutableList<OffchainTransaction> = mutableListOf()
) {
    internal var hubAccount: HubAccount? = null
    internal var hubRoot: HubRoot? = null
    internal var proof: AMTreeProof? = null

    constructor(eon: Int, update: Update) : this(null, eon, update)

    fun addTx(tx: OffchainTransaction) {
        this.txs.add(tx)
    }
}
