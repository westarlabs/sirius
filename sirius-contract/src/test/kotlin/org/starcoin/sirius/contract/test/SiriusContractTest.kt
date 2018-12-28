package org.starcoin.sirius.contract.test

import org.junit.Assert
import org.junit.Test

class SiriusContractTest : ContractTestBase("sirius.sol", "SiriusService") {

    @Test
    fun testGetCurrentEon() {
        val callResult = contract.callConstFunction("getCurrentEon")
        Assert.assertEquals(0, callResult[0] as Int)
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
}
