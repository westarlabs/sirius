package org.starcoin.sirius.contract.test

import org.ethereum.util.blockchain.SolidityCallResult
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.fallback.DefaultCryptoKey
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import kotlin.random.Random

class SiriusContractTest : ContractTestBase("sirius.sol", "SiriusService") {

    private val toUser = DefaultCryptoKey.generateKeyPair()
    private val frUser = DefaultCryptoKey.generateKeyPair()
    private val deposit: Long = 10000


    @Test
    fun test() {
        val callResult = contract.callConstFunction("test")
        val flag = callResult[0] as Boolean
        println(flag)
        Assert.assertTrue(flag)
    }

    @Test
    fun testGetCurrentEon() {
        testCommit()
        val callResult = contract.callConstFunction("getCurrentEon")
        val eon = callResult[0] as BigInteger
        println(eon.longValueExact())
        Assert.assertTrue(eon.longValueExact() > 0)
        println(eon.longValueExact())
    }

    @Test
    fun testDeposit() {
        val callResult = contract.callFunction(deposit, "deposit")
        verifyReturn(callResult)
    }

    @Test
    fun testCommit() {
        createEon(Random.nextInt(0, 10))
    }

    private fun commitData(eon: Int, amount: Long) {
        commitData(eon, amount, true)
    }
    private fun commitData(eon: Int, amount: Long, flag:Boolean) {
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, Direction.ROOT, 0, amount)
        val root = HubRoot(node, eon)
        val data = RLP.dump(HubRoot.serializer(), root)
        val callResult = contract.callFunction("commit", data)
        println(callResult.receipt.error)
        if(flag)
            verifyReturn(callResult)
    }

    @Test
    fun testInitiateWithdrawal() {
        createEon(0)
        val amount: Long = 100
        val eon = 1
        val path = newPath(addr, newUpdate(eon, 1, 0))
        val w = Withdrawal(addr, path, amount)
        val data = RLP.dump(Withdrawal.serializer(), w)
        val callResult = contract.callFunction("initiateWithdrawal", data)
        verifyReturn(callResult)
    }

    @Test
    fun testCancelWithdrawal() {
        val eon = 1
        testInitiateWithdrawal()
        val update = newUpdate(eon, 2, 950)
        val cancel = CancelWithdrawal(Participant(owner.public), update, newPath(addr, update))
        val data = RLP.dump(CancelWithdrawal.serializer(), cancel)
        val callResult = contract.callFunction("cancelWithdrawal", data)
        verifyReturn(callResult)
    }


    @Test
    fun testOpenBalanceUpdateChallenge() {
        val eon = 1
        createEon(1)

        val update1 = newUpdate(eon, 1, 0)//other
        val path = newPath(addr, update1)
        val update2 = newUpdate(eon, 1, 0)//mine
        val leaf2 = newLeaf(addr, update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val bup = BalanceUpdateProof(true, update2, true, amtp)
        val buc = BalanceUpdateChallenge(bup, owner.public)
        val data = RLP.dump(BalanceUpdateChallenge.serializer(), buc)
        val callResult = contract.callFunction("openBalanceUpdateChallenge", data)
        verifyReturn(callResult)
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        val eon = 1
        testOpenBalanceUpdateChallenge()
        val update3 = newUpdate(eon, 3, 0)//other
        val update4 = newUpdate(eon, 4, 0)//mine
        val path = newPath(addr, update3)
        val leaf3 = newLeaf(addr, update4, 1100, 1000)
        val amtp = AMTreeProof(path, leaf3)
        val close = CloseBalanceUpdateChallenge(update4, amtp)
        val data = RLP.dump(CloseBalanceUpdateChallenge.serializer(), close)
        val callResult = contract.callFunction("closeBalanceUpdateChallenge", data)
        verifyReturn(callResult)
    }

    @Test
    fun testOpenTransferDeliveryChallenge() {
        val eon = 1
        createEon(1)
        val update = newUpdate(eon, 1, 0)
        val txData = OffchainTransactionData(eon, addr, addr, 10, 1)
        tx = OffchainTransaction(txData, Signature.of(txData, owner.private))
        val open = OpenTransferDeliveryChallengeRequest(update, tx, MerklePath.mock())
        val data = RLP.dump(OpenTransferDeliveryChallengeRequest.serializer(), open)
        val callResult = contract.callFunction("openTransferDeliveryChallenge", data)
        verifyReturn(callResult)
    }

    @Test
    fun testCloseTransferDeliveryChallenge() {
        val eon = 1
        testOpenTransferDeliveryChallenge()

        val update1 = newUpdate(eon, 1, 0)//other
        val path = newPath(addr, update1)
        val update2 = newUpdate(eon, 1, 0)//mine
        val leaf2 = newLeaf(addr, update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val close =
            CloseTransferDeliveryChallenge(amtp, update2, MerklePath.mock(), owner.public, Hash.of(tx))

        val data = RLP.dump(CloseTransferDeliveryChallenge.serializer(), close)
        val callResult = contract.callFunction("closeTransferDeliveryChallenge", data)
        verifyReturn(callResult)
    }

    @Test
    fun testRecoverFunds() {
//        createEon(2, false)
//
//        val flag = contract.callConstFunction("isRecoveryMode")[0] as Boolean
//        assert(flag)
//        verifyReturn(callResult)
    }

    private fun newUpdate(eon: Int, version: Long, sendAmount: Long): Update {
        val updateData = UpdateData(eon, version, sendAmount, 0, Hash.random())
        return Update(updateData, Signature.of(updateData, owner.private), Signature.of(updateData, owner.private))
    }

    private fun newPath(addr: Address, update: Update): AMTreePath {
        val offset: Long = 100
        val allotment: Long = 1000
        val path = AMTreePath(update.eon, newLeaf(addr, update, offset, allotment))
        for (i in 0..MockUtils.nextInt(0, 10)) {
            path.append(AMTreePathInternalNode.mock())
        }

        return path
    }

    private fun newLeaf(addr: Address, update: Update, offset: Long, allotment: Long): AMTreePathLeafNode {
        val nodeInfo = AMTreeLeafNodeInfo(addr.hash(), update)
        return AMTreePathLeafNode(nodeInfo, Direction.LEFT, offset, allotment)
    }

    private fun createEon(eon: Int) {
        createEon(eon, true)
    }
    private fun createEon(eon: Int, flag:Boolean) {
        var ct = 0
        for (i in 0..eon) {
            val tmp = if(flag) {(4 * (i + 1))} else {(4 * (i + 1)) + 2}
            while (blockHeight.get() < tmp) {
                testDeposit()
                ct += 1
                Thread.sleep(10)
            }
            var total = (ct - 1) * deposit
            commitData(i, total, flag)
        }
    }

    private fun verifyReturn(callResult: SolidityCallResult) {
        Assert.assertTrue(callResult.isSuccessful)
        callResult.receipt.logInfoList.forEach { logInfo ->
            println("event:$logInfo")
        }
    }
}
