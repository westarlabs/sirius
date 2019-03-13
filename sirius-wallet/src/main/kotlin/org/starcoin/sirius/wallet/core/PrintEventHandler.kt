package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*

class PrintEventHandler:ServerEventHandler{

    override fun onHubRootCommit(hubRoot: HubRoot,localVerifyResult : Boolean) {
        println("hub commit root $hubRoot , verify result is $localVerifyResult")
    }

    override fun onDeposit(deposit: Deposit) {
        println("new deposit ${deposit.amount}")
    }

    override fun onWithdrawal(withdrawalStatus: WithdrawalStatus) {
        println("submit withdrawal success $withdrawalStatus")
    }

    override fun onNewTransaction(offchainTransaction: OffchainTransaction) {
        print("handle new transaction,from is ${offchainTransaction.from},value is ${offchainTransaction.amount}," +
                "hash is ${offchainTransaction.hash()}")
    }

    override fun onNewUpdate(update: Update) {
        print("handle new hub signed update")
    }

}