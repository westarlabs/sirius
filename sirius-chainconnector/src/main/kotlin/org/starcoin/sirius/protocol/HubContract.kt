package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.*



interface HubContract {

    fun queryHubInfo(): HubInfo
    fun queryLeastHubCommit(): AugmentedMerkleTree.AugmentedMerkleTreeNode
    fun queryHubCommit(eon: Int): AugmentedMerkleTree.AugmentedMerkleTreeNode
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


    fun recoverFunds(request: RecoverFunds): Hash


}
