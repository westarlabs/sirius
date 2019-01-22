package org.starcoin.sirius.contract

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import kotlin.random.Random

class SiriusContractTest : ContractTestBase("solidity/SiriusService", "SiriusService") {

    private val deposit: Long = 10000
    val ip = "192.168.0.0.1:80"
    val blocksPerEon = 8
    lateinit var preProof: AMTreeProof
    lateinit var currentTree : AMTree


    override fun getContractConstructArg(): Any? {
        return ContractConstructArgs.DEFAULT_ARG.toRLP()
    }

    @Before
    fun zeroEonCommit() {
        //commitData(0, 0, true)
    }

    @Test
    fun test() {
        val callResult = contract.callConstFunction("test")
        val flag = callResult[0] as Boolean
        Assert.assertTrue(flag)
    }

    @Test
    fun testGetCurrentEon() {
        testCommit()
        currentEon()
    }

    private fun currentEon(): Long {
        val callResult = contract.callConstFunction("getCurrentEon")
        val eon = callResult[0] as BigInteger
        LOG.info("eon:$eon")
        Assert.assertTrue(eon.longValueExact() >= 0)
        return eon.longValueExact()
    }

    @Test
    fun testDeposit() {
        testDeposit(true)
    }

    fun testDeposit(flag: Boolean) {
        val callResult = contract.callFunction(deposit, "")
        if (flag)
            verifyReturn(callResult)
        else
            LOG.warning(callResult.receipt.error)
    }

    @Test
    fun testCommit() {
        createEon(Random.nextInt(1, 10))
    }

    @Test
    fun testCommit2() {
        createEon(0, false, false)
    }

    @Test
    fun testCommit3() {
        var ct = 0

        var tmp = (blocksPerEon * 1) + 3

        while (blockHeight.get() < tmp) {
            if (blockHeight.get() < (blocksPerEon) / 2) {
                testDeposit(true)
                ct += 1
            } else {
                sb.createBlock()
            }
        }

        var total = ct * deposit
        commitData(1, total, false)
    }

    @Test
    fun testInitiateWithdrawal() {
        val eon = 1
        val total = createEon(1, true, true)
        val txs = mutableListOf<OffchainTransaction>()
        val updateData = UpdateData(eon, 1)
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)
        val count = 7 * deposit
        val preTree = commitRealData(1, update, total - count, count, true, txs)

        for (i in 0..blocksPerEon) {
            testDeposit(true)
        }

        val amount: BigInteger = BigInteger.valueOf(deposit)

        preProof = preTree.getMembershipProof(callUser.address)!!
        val w = Withdrawal(preProof, amount)
        val data = w.toRLP()
        val callResult = contract.callFunction("initiateWithdrawal", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testCancelWithdrawal() {
        val eon = 1
        testInitiateWithdrawal()
        val update = newUpdate(eon, 2, 135000)
        val cancel =
            CancelWithdrawal(
                callUser.address, update, preProof
            )
        val data = cancel.toRLP()
        val callResult = contract.callFunction("cancelWithdrawal", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testOpenBalanceUpdateChallenge() {
        openBalanceUpdateChallenge(false)
    }

    private fun openBalanceUpdateChallenge(flag: Boolean) {
        val eon = 1
        val total = createEon(eon, true, true)

        val txs = mutableListOf<OffchainTransaction>()
        val update1 = newUpdate(eon, 1, 0)
        val ct = 7
        val count = ct * deposit
        val preTree = commitRealData(eon, update1, total - count, count, true, txs)
        preProof = preTree.getMembershipProof(callUser.address)!!
        for (i in 0..blocksPerEon) {
            testDeposit(true)
        }

        val update2 = newUpdate(eon + 1, 1, deposit)
        currentTree = commitRealData(eon + 1, update2, total, blocksPerEon * deposit, true, txs)

        var update3: Update
        when (flag) {
            true -> update3 = update2
            false -> update3 = newUpdate(eon + 1, 2, 0)
        }
        val bup = BalanceUpdateProof(true, update3, true, preProof.path)

        val data = bup.toRLP()
        val callResult = contract.callFunction("openBalanceUpdateChallenge", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
//        val eon = 1
//        openBalanceUpdateChallenge(true)
//
//        val amtp = currentTree.getMembershipProof(callUser.address)!!
//        val close = CloseBalanceUpdateChallenge(update4, amtp)
//        val data = close.toRLP()
//        val callResult = contract.callFunction("closeBalanceUpdateChallenge", data)
//        assert(callResult.returnValue as Boolean)
//        verifyReturn(callResult)
    }

    @Test
    fun testOpenTransferDeliveryChallenge() {
        val eon = 1
        createEon(1, true, true)
        val txs = mutableListOf<OffchainTransaction>()
        val count = MockUtils.nextLong(10, 20)
        for (i in 0 until count) {
            val txData = OffchainTransactionData(
                eon,
                ethKey2Address(callUser),
                ethKey2Address(callUser), 1, 1
            )
            val txTmp = OffchainTransaction(txData)
            txTmp.sign(callUser)
            txs.add(txTmp)
        }
        val tree = MerkleTree(txs)
        tx = txs[9]

        val updateData = UpdateData(eon, 1, count, count, tree.hash())
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)

        commitRealData(eon, update, 6 * deposit, 7 * deposit, true, txs)

        val open = TransferDeliveryChallenge(update, tx, tree.getMembershipProof(tx.hash()))
        val data = open.toRLP()
        val callResult = contract.callFunction("openTransferDeliveryChallenge", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testCloseTransferDeliveryChallenge() {
        val eon = 1
        testOpenTransferDeliveryChallenge()

        val update1 = newUpdate(eon, 1, 0)//other
        val path = newPath(ethKey2Address(callUser), update1)
        val update2 = newUpdate(eon, 1, 0)//mine
        val leaf2 = newLeafNodeInfo(ethKey2Address(callUser), update2)
        val amtp = AMTreeProof(path, leaf2)
        val close =
            CloseTransferDeliveryChallenge(amtp, update2, MerklePath.mock(), callUser.keyPair.public, Hash.of(tx))

        val data = close.toRLP()
        val callResult = contract.callFunction("closeTransferDeliveryChallenge", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testRecoverFunds() {
        var eon = 2
        createEon(eon, false, false)

        eon = ("" + currentEon()).toInt() - 1
        if (eon < 0)
            eon = 0

        var recovery = contract.callConstFunction("isRecoveryMode")[0] as Boolean
        assert(recovery)
        val update3 = newUpdate(eon, 3, 0)//other
        val update4 = newUpdate(eon, 4, 0)//mine
        val path = newPath(ethKey2Address(callUser), update3)
        val leaf3 = newLeafNodeInfo(ethKey2Address(callUser), update4)

        val refund = AMTreeProof(path, leaf3)
        val data = refund.toRLP()
        val callResult = contract.callFunction("recoverFunds", data)
        verifyReturn(callResult)
    }

    private fun newUpdate(eon: Int, version: Long, sendAmount: Long): Update {
        val updateData = UpdateData(eon, version, sendAmount, 0, Hash.random())
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)
        return update
    }

    private fun newProof(addr: Address, update: Update): AMTreeProof {
        return AMTreeProof(newPath(addr, update), AMTreeLeafNodeInfo.DUMMY_NODE)
    }

    private fun newPath(addr: Address, update: Update): AMTreePath {
        val offset: Long = 100
        val allotment: Long = 1000
        val path = AMTreePath(update.eon, newLeaf(addr, update, offset, allotment))
        for (i in 0..MockUtils.nextInt(0, 10)) {
            path.append(AMTreePathNode.mock())
        }

        return path
    }

    private fun newLeaf(addr: Address, update: Update, offset: Long, allotment: Long): AMTreePathNode {
        val nodeInfo = AMTreeLeafNodeInfo(addr.hash(), update)
        return AMTreePathNode(nodeInfo.hash(), PathDirection.LEFT, offset, allotment)
    }

    fun newLeafNodeInfo(addr: Address, update: Update): AMTreeLeafNodeInfo {
        return AMTreeLeafNodeInfo(addr.hash(), update)
    }

    private fun createEon(eon: Int): Long {
        return createEon(eon, true, false)
    }

    private fun createEon(eon: Int, flag: Boolean, realFlag: Boolean): Long {
        var ct = 0
        var total: Long = 0
        for (i in 0..eon) {
            var tmp = if (!flag && i == eon) {
                (blocksPerEon * (i + 1)) + 2
            } else {
                (blocksPerEon * (i + 1)) - 1
            }

            while (blockHeight.get() < tmp) {
                testDeposit(true)
                ct += 1
            }
            total = ct * deposit
            if (!realFlag || (realFlag && i < eon))
                commitData(i + 1, total, flag)
        }

        return total
    }

    @Test
    fun testHubIp() {
        val callResult = contract.callFunction("hubIp", ip.toByteArray())
        verifyReturn(callResult)
    }

    @Test
    fun testHubInfo() {
        testHubIp()
        val callResult = contract.callConstFunction("hubInfo")[0] as ByteArray
        val obj1 = ContractHubInfo.parseFromRLP(callResult)
        assert(ip == obj1.hubAddress)
    }
}
