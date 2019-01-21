package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*

interface ServerEventHandler {

    fun onHubRootCommit(hubRoot: HubRoot)

    fun onDeposit(deposit: Deposit)

    fun onWithdrawal(withdrawalStatus: WithdrawalStatus)

    fun onNewTransaction(offchainTransaction: OffchainTransaction)

    fun onNewUpdate(update: Update)
}
