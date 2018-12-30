package org.starcoin.sirius.contract.test

import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.serialization.rlp.RLP

class SiriusModelTest : ContractTestBase("model_test.sol", "test_all") {

    @Test
    fun testHubRootDecode() {
        val callResult = contract.callFunction("hub_root_test_decode")

        println(callResult.receipt.error)

        assert(callResult.receipt.isTxStatusOK)

        callResult.receipt.logInfoList.forEach { logInfo ->
            println("event:$logInfo")
        }
    }

    @Test
    fun testHubRoot() {
        val hub = HubRoot.mock()
        val data = RLP.dump(HubRoot.serializer(), hub)
        call(data, "hub_root_test")
    }

    @Test
    fun testInitiateWithdrawal() {
        var w = Withdrawal.mock()

        val data = RLP.dump(Withdrawal.serializer(), w)
        call(data, "initiate_withdrawal_test")
    }

    @Test
    fun testCancelWithdrawal() {
        var w = CancelWithdrawal.mock()

        val data = RLP.dump(CancelWithdrawal.serializer(), w)
        call(data, "cancel_withdrawal_test")
    }

    @Test
    fun testBalanceUpdateChallenge() {
        var w = BalanceUpdateChallenge.mock()

        val data = RLP.dump(BalanceUpdateChallenge.serializer(), w)
        call(data, "balance_update_challenge_test")
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        var close = CloseBalanceUpdateChallenge.mock()
        val data = RLP.dump(CloseBalanceUpdateChallenge.serializer(), close)
        call(data, "close_balance_update_challenge_test")
    }

    @Test
    fun testOpenTransferDeliveryChallengeRequest() {
        var open = OpenTransferDeliveryChallengeRequest.mock()
        val data = RLP.dump(OpenTransferDeliveryChallengeRequest.serializer(), open)
        call(data, "open_pransfer_delivery_challenge_request_test")
    }

    @Test
    fun testCloseTransferDeliveryChallenge() {
        var cloes = CloseTransferDeliveryChallenge.mock()
        val data = RLP.dump(CloseTransferDeliveryChallenge.serializer(), cloes)
        call(data, "close_transfer_delivery_challenge_test")
    }

    @Test
    fun testAMTreeProof() {
        var proof = AMTreeProof.mock()
        val data = RLP.dump(AMTreeProof.serializer(), proof)
        call(data, "am_tree_proof_test")
    }

    @Test
    fun testAMTreePathLeafNode() {
        var leaf = AMTreePathLeafNode.mock()

        val data = RLP.dump(AMTreePathLeafNode.serializer(), leaf)
        call(data, "am_tree_path_leaf_node")
    }

    @Test
    fun testBalanceUpdateProof() {
        var proof = BalanceUpdateProof.mock()

        val data = RLP.dump(BalanceUpdateProof.serializer(), proof)
        call(data, "balance_update_proof_test")
    }
}