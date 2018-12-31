package org.starcoin.sirius.contract.test

import org.junit.Assert
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

        val callResult = contract.callConstFunction("hub_root_test", data)[0] as ByteArray
        var tmp = RLP.load(HubRoot.serializer(), callResult)

        assert(hub.equals(tmp))
    }

    @Test
    fun testInitiateWithdrawal() {
        var w = Withdrawal.mock()

        val data = RLP.dump(Withdrawal.serializer(), w)

        val callResult = contract.callConstFunction("initiate_withdrawal_test", data)[0] as ByteArray
        var tmp = RLP.load(Withdrawal.serializer(), callResult)

        assert(w.equals(tmp))
    }

    @Test
    fun testCancelWithdrawal() {
        var w = CancelWithdrawal.mock()

        val data = RLP.dump(CancelWithdrawal.serializer(), w)

        val callResult = contract.callConstFunction("cancel_withdrawal_test", data)[0] as ByteArray
        var tmp = RLP.load(CancelWithdrawal.serializer(), callResult)

        assert(w.equals(tmp))
    }

    @Test
    fun testBalanceUpdateChallenge() {
        var b = BalanceUpdateChallenge.mock()

        val data = RLP.dump(BalanceUpdateChallenge.serializer(), b)

        val callResult = contract.callConstFunction("balance_update_challenge_test", data)[0] as ByteArray
        var tmp = RLP.load(BalanceUpdateChallenge.serializer(), callResult)

        assert(b.equals(tmp))
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        var close = CloseBalanceUpdateChallenge.mock()
        val data = RLP.dump(CloseBalanceUpdateChallenge.serializer(), close)

        val callResult = contract.callConstFunction("close_balance_update_challenge_test", data)[0] as ByteArray
        var tmp = RLP.load(CloseBalanceUpdateChallenge.serializer(), callResult)

        assert(close.equals(tmp))
    }

    @Test
    fun testOpenTransferDeliveryChallengeRequest() {
        var open = OpenTransferDeliveryChallengeRequest.mock()
        val data = RLP.dump(OpenTransferDeliveryChallengeRequest.serializer(), open)

        val callResult =
            contract.callConstFunction("open_transfer_delivery_challenge_request_test", data)[0] as ByteArray
        var tmp = RLP.load(OpenTransferDeliveryChallengeRequest.serializer(), callResult)

        assert(open.equals(tmp))
    }

    @Test
    fun testCloseTransferDeliveryChallenge() {
        var close = CloseTransferDeliveryChallenge.mock()
        val data = RLP.dump(CloseTransferDeliveryChallenge.serializer(), close)

        val callResult = contract.callConstFunction("close_transfer_delivery_challenge_test", data)[0] as ByteArray
        var tmp = RLP.load(CloseTransferDeliveryChallenge.serializer(), callResult)

        assert(close.equals(tmp))
    }

    @Test
    fun testAMTreeProof() {
        var proof = AMTreeProof.mock()
        val data = RLP.dump(AMTreeProof.serializer(), proof)
        val callResult = contract.callConstFunction("am_tree_proof_test", data)[0] as ByteArray
        var tmp = RLP.load(AMTreeProof.serializer(), callResult)

        assert(proof.equals(tmp))
    }

    @Test
    fun testAMTreePathLeafNode() {
        var leaf = AMTreePathLeafNode.mock()
        LOG.info(leaf.toJSON())
        val data = RLP.dump(AMTreePathLeafNode.serializer(), leaf)
        val callResult = contract.callConstFunction("am_tree_path_leaf_node_test", data)[0] as ByteArray
        //LOG.info(Utils.HEX.encode(data))
        //LOG.info(Utils.HEX.encode(callResult))
        Assert.assertArrayEquals(data, callResult)
        var tmp = RLP.load(AMTreePathLeafNode.serializer(), callResult)

        assert(leaf.equals(tmp))
    }

    @Test
    fun testBalanceUpdateProof() {
        var proof = BalanceUpdateProof.mock()

        val data = RLP.dump(BalanceUpdateProof.serializer(), proof)
        val callResult = contract.callConstFunction("balance_update_proof_test", data)[0] as ByteArray
        var tmp = RLP.load(BalanceUpdateProof.serializer(), callResult)

        assert(proof.equals(tmp))
    }
}
