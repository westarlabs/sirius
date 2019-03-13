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
import org.starcoin.sirius.lang.hexToByteArray

class SiriusModelTest : ContractTestBase("solidity/test_all", "test_all") {

    fun <T : SiriusObject> doTest(siriusClass: KClass<T>, functionName: String) = runBlocking {
        LOG.info("doTest $siriusClass")
        val companion = siriusClass.companionObjectInstance as SiriusObjectCompanion<*, *>
        retryUntilTrueWithTimeout(5000) {
            try {
                val obj = companion.mock()
                doTest(obj, functionName)
                true
            } catch (e: Error) {
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
    fun testCancelWithdrawal4Bytes() {
        test4Bytes(
            "cancel_withdrawal_test2",
            CancelWithdrawal::class,
            "f904269400dbe094cabc9b9806f8566f99df3b6c89e2f274f8cbf83c830b7b3384010a343688172eec79fe5ba8008879d6a4dd98316000a0c86d2385a7adda6962e6545985906f249c2015538e0cbe465408ce1525d06006b845f8431ba02d9c8ee492ed1f38309e33b884a223452e2e31d183097d839d82907d608e5700a06b959a8392b402837ac0171761a4d77ec23ec48742cd26d239756a3d8b0e6dbfb844f8421b9f090f2f8153bb227d410795445f956e3554a94fde738b1e90539f270ecf0cf4a04cd927d5fbb1689cbeff575b30a48d19af7b9f584b8827856253dcaa4de9c4a4f90341f9024e8306fd37f4a09175f7bf156f5e1709238f321eb366160713dc1c5e0474e6caf55966bc34943b01887d691ad0a9b98000880cd539ce84c10c00f90212f4a0a0c907156202bdab166215b3299e9875400744f3a68e01c26831537dc533f3cb01882f4a08cc2ceeb8008847d993a421c5cc00f4a02831c3b1a7e250541f2d82de5eef3bf7f6bfcf3d71a9d247bf21d3779f856a1f0288369decbc6c91c4008878eb73f21c1d9800f4a07573c0c7c8eb517237b647581a32cc97028dd6998518ca02b9527ce31d279962808801bfe7820a6bac008855d8ff4b414b5000f4a0c7ce3f4ce07e9ab3d2f89c4c1c4a08c7c4c89227fae60e489589bbc302ad62eb01885733681b932fa800884754cc3ce0d98800f4a0676944dd0f2d225d86c6fd7004f64f62198ad910d5a2fd3b7fa39c3a69ba007601885c63381f3abf1c008871be55f22a956400f4a02f20413617237426d678bdb0b2264dafea642a73edde4bb24bba83a68134fe0702885628911f82af9c0088072b30a0758e3400f4a07c6da5d4546e18c5e1a452f0054f74dbcfa3971aeb5e6b684a96c3401388ba1d8088710d2683dd4e6c008845c308e2608f4000f4a0cf4b0f68e8589dd8d88de81914d4abae7fc9cfa127d11b7571258871a9d1e030018867992f36c410f000883392ca4d073cd000f4a016c358d44bef3634bc364440a2d77d3b4ceea74a5af78f1b4779cb85b558b3d780880102489308324c00882adf6c5c3a18dc00f4a00bb868a6e2f2052de041eaf906b6940e416cb9bd950b26e32bd6df50d7bdc3ae01885775ccb7684b6c008835e8808fd7700400f8eea00258323b2eaa700f7290ff21cb55542312f84347d402d057f296bd749dd0ec67f8cbf83b831dbf3883b4aa1f881aa49993ab2420008814a621a10bf19000a08642356fa9996aecf29003c35fa845a1f48463f4fa52ca5cad5eb844eb330687b845f8431ca04dc42a33f4e142b77ec301dc8daccedbbf902aa05c1ca82da004e9b64beff6c9a01a96b140f12cd54881b157b77a60bfbe8ae564bdf8e194b370e427113592b83db845f8431ca08b8d448f6947286c458b5d647831cbe347d3fdec8ea37e2cdf8951482e1404f3a04bf707dac85af0c8a4eeee0b26aafa78d3f5e96f78d699e8c63a02e3368c89cc"
        )
    }

    fun <T : SiriusObject> test4Bytes(method: String, clazz: KClass<T>, str: String) {
        val data = str.hexToByteArray()
        val codec = SiriusObjectRLPCodec(clazz)
        val cw = codec.decode(data)
        println(cw.toString())
        val callResult = contract.callConstFunction(method, data)[0] as ByteArray
        Assert.assertArrayEquals("expect ${data.toHEXString()} but get ${callResult.toHEXString()}", data, callResult)
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

        val callResult = contract.callFunction(
            "verify_merkle_test2", tree.getRoot().hash().toBytes(),
            txs[0].hash().toBytes(),
            txs[1].hash().toBytes()
        )
        callResult.receipt.logInfoList.forEach { logInfo ->
            LOG.info("event:$logInfo")
        }
    }
}
