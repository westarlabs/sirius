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

    private fun call(data: ByteArray, method: String) {
        val dataStr = bytesToHexString(data)!!

        val callResult = contract.callFunction(method, data)

        println(callResult.receipt.error)

        val resultStr = bytesToHexString(callResult.receipt.executionResult)!!

        assert(callResult.receipt.isTxStatusOK)

        println(dataStr)

        println(resultStr)
    }

    private fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.size <= 0) {
            return null
        }
        for (i in 0..src.size - 1) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }
}