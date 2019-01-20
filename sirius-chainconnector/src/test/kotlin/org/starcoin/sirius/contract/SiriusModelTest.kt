package org.starcoin.sirius.contract

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toHEXString
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

class SiriusModelTest : ContractTestBase("solidity/test_all", "test_all") {

    fun <T : SiriusObject> doTest(siriusClass: KClass<T>, functionName: String) {
        val companion = siriusClass.companionObjectInstance as SiriusObjectCompanion<*, *>
        val obj = companion.mock()
        val data = obj.toRLP()
        val callResult = contract.callConstFunction(functionName, data)[0] as ByteArray
        Assert.assertArrayEquals("expect ${data.toHEXString()} but get ${callResult.toHEXString()}", data, callResult)
        val obj1 = companion.parseFromRLP(callResult)
        Assert.assertEquals(obj, obj1)
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
        doTest(TransferDeliveryChallenge::class, "open_transfer_delivery_challenge_request_test")
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
    fun testAMTreePathNode() {
        doTest(AMTreePathNode::class, "am_tree_path_node_test")
    }

    @Test
    fun testBalanceUpdateProof() {
        doTest(BalanceUpdateProof::class, "balance_update_proof_test")
    }

    @Test
    fun testContractReturn() {
        doTest(ContractReturn::class, "contract_return_test")
    }

    @Test
    fun testUpdateData() {
        doTest(UpdateData::class, "update_data_test")
    }

    @Test
    fun testUpdate() {
        doTest(Update::class, "update_test")
    }

    @Test
    fun testAMTreeProof2() {
        val tree = AMTree.random()
        val obj = tree.randommProof as AMTreeProof
        val data1 = obj.toRLP()
        val callResult1 = contract.callConstFunction("am_tree_proof_test", data1)[0] as ByteArray
        Assert.assertArrayEquals(
            "expect111 ${data1.toHEXString()} but get ${callResult1.toHEXString()}",
            data1,
            callResult1
        )
        val obj1 = AMTreeProof.parseFromRLP(callResult1)
        Assert.assertEquals(obj, obj1)

        val root = tree.root.toAMTreePathNode()
        val data2 = root.toRLP()
        Assert.assertTrue(AMTree.verifyMembershipProof(tree.root.toAMTreePathNode(), obj))
        val callResult2 = contract.callConstFunction("am_tree_proof_test2", data1, data2)[0] as ByteArray
        Assert.assertArrayEquals(
            "expect222 ${data2.toHEXString()} but get ${callResult2.toHEXString()}",
            data2,
            callResult2
        )
//        val obj2 = AMTreeProof.parseFromRLP(callResult2)
//        Assert.assertEquals(root, obj2)
    }
}
