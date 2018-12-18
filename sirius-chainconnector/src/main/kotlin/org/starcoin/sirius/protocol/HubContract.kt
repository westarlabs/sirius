package org.starcoin.sirius.protocol

import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.AugmentedMerkleTree
import org.starcoin.sirius.core.HubInfo

interface HubContract {

    fun queryHubInfo():HubInfo
    fun queryLeastHubCommit():AugmentedMerkleTree.AugmentedMerkleTreeNode
    fun queryHubCommit(request: Starcoin.QueryHubCommitRequest):AugmentedMerkleTree.AugmentedMerkleTreeNode

    fun initiateWithdrawal(request:Starcoin.InitiateWithdrawalRequest)
    fun cancelWithdrawal(request: Starcoin.CancelWithdrawalRequest)
    fun openBalanceUpdateChallenge(request:Starcoin.ProtoBalanceUpdateChallenge)
    fun queryCurrentBalanceUpdateChallenge(request:Starcoin.QueryBalanceUpdateChallengesRequest)
    fun closeBalanceUpdateChallenge (request: Starcoin.CloseBalanceUpdateChallengeRequest)
    fun commit (request:Starcoin.ProtoHubRoot)
    fun openTransferDeliveryChallenge (request:Starcoin.OpenTransferDeliveryChallengeRequest)
    fun closeTransferDeliveryChallenge (request:Starcoin.CloseTransferDeliveryChallengeRequest)
    fun queryCurrentTransferDeliveryChallenges (request:Starcoin.QueryCurrentTransferDeliveryChallengesRequest) : Starcoin.QueryCurrentTransferDeliveryChallengesResponse
    fun recoverFunds (request:Starcoin.RecoverFundsRequest)
    fun queryWithdrawalStatus (request:Starcoin.QueryWithdrawalStatusRequest): Starcoin.ProtoWithdrawalStatus

}