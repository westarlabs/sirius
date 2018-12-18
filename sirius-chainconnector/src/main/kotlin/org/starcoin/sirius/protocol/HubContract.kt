package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.*



interface HubContract {

    fun queryHubInfo(): HubInfo
    fun queryLeastHubCommit(): AugmentedMerkleTree.AugmentedMerkleTreeNode
    fun queryHubCommit(eon: Int): AugmentedMerkleTree.AugmentedMerkleTreeNode
    fun queryCurrentBalanceUpdateChallenge(address: BlockAddress): BalanceUpdateChallenge
    fun queryCurrentTransferDeliveryChallenge(address: BlockAddress): TransferDeliveryChallenge
    fun queryWithdrawalStatus(address: BlockAddress): WithdrawalStatus

    fun initiateWithdrawal(request: InitiateWithdrawal): Hash
    fun cancelWithdrawal(request: CancelWithdrawal): Hash

    fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash
    fun closeBalanceUpdateChallenge(request: CloseBalanceUpdateChallenge): Hash

    fun commit(request: HubRoot): Hash

    fun openTransferDeliveryChallenge(request: TransferDeliveryChallenge): Hash
    fun closeTransferDeliveryChallenge(request: CloseTransferDeliveryChallenge): Hash


    fun recoverFunds(request: RecoverFunds): Hash


}