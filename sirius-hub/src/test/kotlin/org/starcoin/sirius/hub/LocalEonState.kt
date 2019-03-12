package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*

enum class DepositStatusType {
    Init,
    ConfirmedByChain,
    ConfirmedByHub,
}

data class DepositStatus(val type: DepositStatusType, val deposit: Deposit, val txHash: Hash)

data class LocalEonState(
    var previous: LocalEonState?,
    val eon: Int,
    var update: Update,
    val txs: MutableList<OffchainTransaction> = mutableListOf()
) {
    internal var hubAccount: HubAccount? = null
    internal var hubRoot: HubRoot? = null
    internal var proof: AMTreeProof? = null
    internal val deposits: MutableList<DepositStatus> = mutableListOf()
    internal var withdrawalStatus: WithdrawalStatus? = null

    constructor(eon: Int, update: Update) : this(null, eon, update)

    constructor(
        previous: LocalEonState,
        eon: Int,
        update: Update,
        hubRoot: HubRoot,
        hubAccount: HubAccount,
        proof: AMTreeProof
    ) : this(previous, eon, update) {
        this.hubRoot = hubRoot
        this.hubAccount = hubAccount
        this.proof = proof
    }

    fun addTx(tx: OffchainTransaction) {
        this.txs.add(tx)
    }
}
