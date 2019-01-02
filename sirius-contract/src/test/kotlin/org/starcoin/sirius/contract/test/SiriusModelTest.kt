package org.starcoin.sirius.contract.test

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toHEXString
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

class SiriusModelTest : ContractTestBase("model_test.sol", "test_all") {

    fun <T : SiriusObject> doTest(siriusClass: KClass<T>, functionName: String) {
        val companion = siriusClass.companionObjectInstance as SiriusObjectCompanion<*, *>
        val obj = companion.mock()
        val data = obj.toRLP()
        val callResult = contract.callConstFunction(functionName, data)[0] as ByteArray
        val obj1 = companion.parseFromRLP(callResult)
        Assert.assertArrayEquals("expect ${data.toHEXString()} but get ${callResult.toHEXString()}", data, callResult)
        Assert.assertEquals(obj, obj1)
    }

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
        doTest(HubRoot::class, "hub_root_test")
    }

    @Test
    fun testInitiateWithdrawal() {
        doTest(Withdrawal::class, "initiate_withdrawal_test")
    }

    @Test
    fun testCancelWithdrawal() {
        doTest(CancelWithdrawal::class, "cancel_withdrawal_test")
    }

    @Test
    fun testBalanceUpdateChallenge() {
        doTest(BalanceUpdateChallenge::class, "balance_update_challenge_test")
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        doTest(CloseBalanceUpdateChallenge::class, "close_balance_update_challenge_test")
    }

    @Test
    fun testOpenTransferDeliveryChallengeRequest() {
        doTest(OpenTransferDeliveryChallengeRequest::class, "open_transfer_delivery_challenge_request_test")
    }

    @Test
    fun testCloseTransferDeliveryChallenge() {
        doTest(CloseTransferDeliveryChallenge::class, "close_transfer_delivery_challenge_test")
    }

    @Test
    fun testAMTreeProof() {
        doTest(AMTreeProof::class, "am_tree_proof_test")
    }

    @Test
    fun testAMTreePathLeafNode() {
        doTest(AMTreePathLeafNode::class, "am_tree_path_leaf_node_test")
    }

    @Test
    fun testBalanceUpdateProof() {
        doTest(BalanceUpdateProof::class, "balance_update_proof_test")
    }
}
