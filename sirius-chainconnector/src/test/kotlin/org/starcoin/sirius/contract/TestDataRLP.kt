package org.starcoin.sirius.contract

import org.ethereum.core.CallTransaction
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.rlp.RLP
import java.math.BigInteger

class TestDataRLP : ContractTestBase("solidity/TestDataRLP", "TestDataRLP") {

    @Test
    fun testBool() {
        val callResult = contract.callConstFunction("testBoolTrue")
        Assert.assertTrue(callResult[0] as Boolean)

        val callResult1 = contract.callConstFunction("testBoolFalse")
        Assert.assertFalse(callResult1[0] as Boolean)
    }

    @Test
    fun testEcho() {
        val data = Data.random()
        val dataRLP = RLP.dump(Data.serializer(), data)
        val callResult = contract.callConstFunction("echo", dataRLP)
        Assert.assertTrue(callResult.isNotEmpty())
        val returnDataRLP = callResult[0] as ByteArray
        Assert.assertArrayEquals(dataRLP, returnDataRLP)
        val returnData = RLP.load(Data.serializer(), returnDataRLP)
        Assert.assertEquals(data, returnData)
    }

    @Test
    fun testDataSetAndGet() {
        val data = Data.random(true)
        doTestDataSetAndGet(data)
        val data1 = Data.random(false)
        doTestDataSetAndGet(data1)
    }

    @Test
    fun testDataSetAndGetDefaultValue() {
        val data = Data(false, 0, "", Address.ZERO_ADDRESS, BigInteger.ZERO)
        doTestDataSetAndGet(data)
    }

    @Test
    fun testDataSetAndGetSpecialValue() {
        val data = Data(false, 0, "", Address.ZERO_ADDRESS, 1000000000000.toBigInteger())
        doTestDataSetAndGet(data)
    }

    fun doTestDataSetAndGet(data: Data) {
        LOG.info("data:$data")
        val dataRLP = RLP.dump(Data.serializer(), data)
        val setResult = contract.callFunction("set", dataRLP)
        setResult.receipt.logInfoList.forEach { logInfo ->
            val contract = CallTransaction.Contract(contract.abi)
            val invocation = contract.parseEvent(logInfo)
            println("event:$invocation")
        }
        Assert.assertTrue(setResult.isSuccessful)

        val callResult = contract.callConstFunction("get")
        Assert.assertTrue(callResult.isNotEmpty())
        val returnDataRLP = callResult[0] as ByteArray
//        println("${dataRLP.size}:${returnDataRLP.size}")
//        println(bytesToHexString(dataRLP))
//        println(bytesToHexString(returnDataRLP))
        Assert.assertEquals(dataRLP.toHEXString(), returnDataRLP.toHEXString())
        Assert.assertArrayEquals(dataRLP, returnDataRLP)
        val returnData = RLP.load(Data.serializer(), returnDataRLP)
        Assert.assertEquals(data, returnData)

    }
}
