package org.starcoin.sirius.contract.test

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import java.math.BigInteger
import kotlin.random.Random

class SiriusContractTest : ContractTestBase("sirius.sol", "SiriusService") {

    @Test
    fun testGetCurrentEon() {
        val callResult = contract.callConstFunction("getCurrentEon")
        Assert.assertEquals(0, callResult[0] as BigInteger)
    }

    @Test
    fun testDeposit() {
        val amount: Long = 10
        val setResult = contract.callFunction(amount, "deposit", amount)

        Assert.assertTrue(setResult.isSuccessful)
        setResult.receipt.logInfoList.forEach { logInfo ->
            println("event:$logInfo")
        }
    }

    @Test
    fun testCommit() {
        val amout: Long = Random(Long.MAX_VALUE).nextLong()
        val info: AMTreeInternalNodeInfo = AMTreeInternalNodeInfo(Hash.random(), amout, Hash.random())
        val node: AMTreePathInternalNode = AMTreePathInternalNode(info, Direction.ROOT, 0, amout)
        val root: HubRoot = HubRoot(node, 1)
    }
}
