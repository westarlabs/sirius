package org.starcoin.sirius.contract.test

import org.junit.Assert
import org.junit.Test

class SiriusContractTest : ContractTestBase("sirius.sol", "SiriusService") {

    @Test
    fun testCurrentEon() {
        val callResult = contract.callConstFunction("getCurrentEon")
        Assert.assertEquals(0, callResult[0] as Int)
    }
}
