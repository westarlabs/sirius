package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.*

interface HubContract {

    fun queryHubInfo(): ContractHubInfo
    fun queryLeastHubCommit(): HubRoot?
    fun queryHubCommit(eon: Int): HubRoot?
    fun queryCurrentBalanceUpdateChallenge(address: Address): BalanceUpdateChallenge?
    fun queryCurrentTransferDeliveryChallenge(address: Address): TransferDeliveryChallenge?
    fun queryWithdrawalStatus(address: Address): WithdrawalStatus?

    fun initiateWithdrawal(request: Withdrawal): Hash
    fun cancelWithdrawal(request: CancelWithdrawal): Hash

    fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash
    fun closeBalanceUpdateChallenge(request: CloseBalanceUpdateChallenge): Hash

    fun commit(request: HubRoot): Hash

    fun openTransferDeliveryChallenge(request: TransferDeliveryChallenge): Hash
    fun closeTransferDeliveryChallenge(request: CloseTransferDeliveryChallenge): Hash

    fun recoverFunds(request: AMTreeProof): Hash

    fun getContractAddr():ByteArray
}
