package org.starcoin.sirius.protocol

import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*



interface HubContract {

    fun queryHubInfo(): HubInfo
    fun queryLeastHubCommit(): HubRoot
    fun queryHubCommit(eon: Int): HubRoot
    fun queryCurrentBalanceUpdateChallenge(address: Address): BalanceUpdateChallenge
    fun queryCurrentTransferDeliveryChallenge(address: Address): TransferDeliveryChallenge
    fun queryWithdrawalStatus(address: Address): WithdrawalStatus

    fun initiateWithdrawal(request: Withdrawal): Hash
    fun cancelWithdrawal(request: CancelWithdrawal): Hash

    fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash
    fun closeBalanceUpdateChallenge(request: BalanceUpdateProof): Hash

    fun commit(request: HubRoot): Hash

    fun openTransferDeliveryChallenge(request: TransferDeliveryChallenge): Hash
    fun closeTransferDeliveryChallenge(request: CloseTransferDeliveryChallenge): Hash

    fun recoverFunds(request: AMTreeProof): Hash

    fun getContractAddr():ByteArray
}
