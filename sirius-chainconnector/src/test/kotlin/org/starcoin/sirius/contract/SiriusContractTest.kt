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

class SiriusContractTest : ContractTestBase("/solidity/sirius.sol", "SiriusService") {

    private val deposit: Long = 10000
    val ip = "192.168.0.0.1:80"
    val blocksPerEon = 8


    override fun getContractConstructArg(): Any? {
        val info = AMTreeInternalNodeInfo(Hash.random(), 0, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0, 0)
        val root = HubRoot(node, 0)

        val data = RLP.dump(ContractConstructArgs.serializer(), ContractConstructArgs(8, root))
        return data
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
        createEon(0)
        val amount: Long = 100
        val eon = 1
        val path = newProof(ethKey2Address(callUser), newUpdate(eon, 1, 0))
        val w = Withdrawal(ethKey2Address(callUser), path, amount)
        val data = RLP.dump(Withdrawal.serializer(), w)
        val callResult = contract.callFunction("initiateWithdrawal", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testCancelWithdrawal() {
        val eon = 1
        testInitiateWithdrawal()
        val update = newUpdate(eon, 2, 950)
        val cancel =
            CancelWithdrawal(
                callUser.address, update, newProof(
                    ethKey2Address(
                        callUser
                    ), update
                )
            )
        val data = RLP.dump(CancelWithdrawal.serializer(), cancel)
        val callResult = contract.callFunction("cancelWithdrawal", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testOpenBalanceUpdateChallenge() {
        val eon = 1
        createEon(1)

        val update1 = newUpdate(eon, 1, 0)//other
        val path = newPath(ethKey2Address(callUser), update1)
        val update2 = newUpdate(eon, 1, 0)//mine
        val leaf2 = newLeaf(ethKey2Address(callUser), update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val bup = BalanceUpdateProof(true, update2, true, amtp)
        val buc = BalanceUpdateChallenge(bup, callUser.keyPair.public)
        val data = RLP.dump(BalanceUpdateChallenge.serializer(), buc)
        val callResult = contract.callFunction("openBalanceUpdateChallenge", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
    }

    @Test
    fun testCloseBalanceUpdateChallenge() {
        val eon = 1
        testOpenBalanceUpdateChallenge()
        val update3 = newUpdate(eon, 3, 0)//other
        val update4 = newUpdate(eon, 4, 0)//mine
        val path = newPath(ethKey2Address(callUser), update3)
        val leaf3 = newLeaf(ethKey2Address(callUser), update4, 1100, 1000)
        val amtp = AMTreeProof(path, leaf3)
        val close = CloseBalanceUpdateChallenge(update4, amtp)
        val data = RLP.dump(CloseBalanceUpdateChallenge.serializer(), close)
        val callResult = contract.callFunction("closeBalanceUpdateChallenge", data)
        assert(callResult.returnValue as Boolean)
        verifyReturn(callResult)
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
        val data = RLP.dump(TransferDeliveryChallenge.serializer(), open)
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
        val leaf2 = newLeaf(ethKey2Address(callUser), update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val close =
            CloseTransferDeliveryChallenge(amtp, update2, MerklePath.mock(), callUser.keyPair.public, Hash.of(tx))

        val data = RLP.dump(CloseTransferDeliveryChallenge.serializer(), close)
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
        val leaf3 = newLeaf(ethKey2Address(callUser), update4, 0, 0)

        val refund = AMTreeProof(path, leaf3)
        val data = RLP.dump(AMTreeProof.serializer(), refund)
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
        return AMTreeProof(newPath(addr, update), AMTreePathLeafNode.DUMMY_NODE)
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
        return AMTreePathLeafNode(nodeInfo, PathDirection.LEFT, offset, allotment)
    }

    private fun createEon(eon: Int) {
        createEon(eon, true, false)
    }

    private fun createEon(eon: Int, flag: Boolean, realFlag: Boolean) {
        var ct = 0

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
            var total = ct * deposit
            if (!realFlag || (realFlag && i < eon))
                commitData(i + 1, total, flag)
        }
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
