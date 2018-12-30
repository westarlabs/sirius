package org.starcoin.sirius.contract.test

import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.serialization.rlp.RLP

class SiriusModelTest : ContractTestBase("model_test.sol", "test_all") {

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
        val hub = HubRoot.mock()
        val data = RLP.dump(HubRoot.serializer(), hub)
        call(data, "hub_root_test")
    }

    @Test
    fun testInitiateWithdrawal() {
        var w = Withdrawal.mock()

        val data = RLP.dump(Withdrawal.serializer(), w)
        call(data, "initiate_withdrawal_test")
    }
}