package org.starcoin.sirius.contract

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.retryUntilTrueWithTimeout
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.util.MockUtils
import org.starcoin.sirius.util.error
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

class SiriusModelTest : ContractTestBase("solidity/test_all", "test_all") {

    fun <T : SiriusObject> doTest(siriusClass: KClass<T>, functionName: String) = runBlocking {
        LOG.info("doTest $siriusClass")
        val companion = siriusClass.companionObjectInstance as SiriusObjectCompanion<*, *>
        retryUntilTrueWithTimeout(5000) {
            try {
                val obj = companion.mock()
                doTest(obj, functionName)
                true
            } catch (e: Exception) {
                LOG.error(e)
                false
            }
        }

    }

    fun <T : SiriusObject> doTest(obj: T, functionName: String) = runBlocking {
        LOG.info("doTest $obj")
        val companion = obj.javaClass.kotlin.companionObjectInstance as SiriusObjectCompanion<*, *>
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
        doTest(BalanceUpdateProof(Update.mock()), "balance_update_proof_test")
        doTest(BalanceUpdateProof(AMTreePath.mock()), "balance_update_proof_test")
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        doTest(CloseBalanceUpdateChallenge::class, "close_balance_update_challenge_test")
        doTest(CloseBalanceUpdateChallenge(Address.random(), AMTreeProof.mock()), "close_balance_update_challenge_test")
        doTest(
            CloseBalanceUpdateChallenge(Address.DUMMY_ADDRESS, AMTreeProof.DUMMY_PROOF),
            "close_balance_update_challenge_test"
        )
    }

    @Test
    fun testAMTreePath() {
        doTest(AMTreePath::class, "am_tree_path_test")
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
    fun testVerifyAMTreeProof() {
        val tree = AMTree.random()
        val obj = tree.randomProof as AMTreeProof
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
        val flag = contract.callConstFunction("verify_proof_test", data1, data2)[0] as Boolean
        Assert.assertTrue(flag)
    }

    @Test
    fun testVerifyMerkle() {
        val eon = 1
        val txs = mutableListOf<OffchainTransaction>()
        val txCount = MockUtils.nextLong(10, 20)
        for (i in 0 until txCount) {
            val txData = OffchainTransactionData(
                eon,
                callUser.address,
                callUser.address, 1, 1
            )
            val txTmp = OffchainTransaction(txData)
            txTmp.sign(callUser)
            txs.add(txTmp)
        }
        val tree = MerkleTree(txs)
        tx = txs[9]

        val updateData = UpdateData(eon, 1, txCount, txCount, tree.hash())
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)

        val accounts = mutableListOf<HubAccount>()
        accounts.add(HubAccount(callUser.keyPair.public, update, 100, 0, 0, txs))
        val am = AMTree(eon + 1, accounts)

        val close = CloseTransferDeliveryChallenge(
            am.getMembershipProofOrNull(callUser.address)!!,
            tree.getMembershipProof(tx.hash())!!,
            callUser.address,
            tx.hash()
        )
        val flag = contract.callConstFunction("verify_merkle_test", close.toRLP())[0] as Boolean
        Assert.assertTrue(flag)
    }

    @Test
    fun testVerifyMerkle2() {
        val txs = mutableListOf<OffchainTransaction>()
        for (i in 0 until 2) {
            val txData = OffchainTransactionData(
                1,
                callUser.address,
                callUser.address, 1, 1
            )
            val txTmp = OffchainTransaction(txData)
            txTmp.sign(callUser)
            txs.add(txTmp)
        }
        val tree = MerkleTree(txs)

        val callResult = contract.callFunction("verify_merkle_test2", tree.getRoot().hash().toBytes(),
            txs[0].hash().toBytes(),
            txs[1].hash().toBytes())
        callResult.receipt.logInfoList.forEach { logInfo ->
            LOG.info("event:$logInfo")
        }
    }
}
