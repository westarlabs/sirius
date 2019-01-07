package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.starcoin.sirius.serialization.NamedData
import org.starcoin.sirius.serialization.TestData
import org.starcoin.sirius.util.Utils
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

@Serializable
data class TestCollectionData(val name: String, val list: List<TestData>)

@ImplicitReflectionSerializer
class RLPTest {
    companion object : WithLogging() {

    }

    @Test
    fun testRLPInputOutput() {
        val data = TestData.random()

        var rlpList = RLPList(mutableListOf())
        val output = RLPOutput(rlpList, true)
        output.encode(data)

        Assert.assertTrue(rlpList.isNotEmpty())

        var bytes = rlpList.encode()
        var rlpList1 = bytes.decodeRLP() as RLPList

        Assert.assertEquals(rlpList.size, rlpList1.size)

        val input = RLPInput(rlpList1, true)
        val data1 = input.decode(TestData.serializer())
        Assert.assertEquals(data, data1)
    }


    @Test
    fun testObjectNestMany() {
        0.rangeTo(10000).forEach {
            //println(it)
            testObjectNest()
        }
    }

    @Test
    fun testObjectNestJson() {
        val data = TestData.random()
        val namedData = NamedData("test", data)
        val jsonString = JSON.stringify(namedData)
        //println(jsonString)
        val namedData1 = JSON.parse<NamedData>(jsonString)
        Assert.assertEquals(namedData, namedData1)
    }

    @Test
    fun testObjectNest() {
        val data = TestData.random()
        val namedData = NamedData("test", data)
        var rlpList = RLPList(mutableListOf())
        val output = RLPOutput(rlpList, true)
        output.encode(namedData)

        Assert.assertTrue(rlpList.isNotEmpty())

        var bytes = rlpList.encode()
        var rlpList1 = bytes.decodeRLP() as RLPList

        Assert.assertEquals(rlpList.size, rlpList1.size)

        val input = RLPInput(rlpList1, true)
        val namedData1 = input.decode(NamedData.serializer())
        Assert.assertEquals(namedData, namedData1)
    }

    val specialBigIntegerValues =
        listOf(BigInteger.ZERO, BigInteger.ONE, 1000000000000.toBigInteger(), 10835967.toBigInteger())

    @Test
    fun testBigIntegerRLP() {
        for (bigInteger in specialBigIntegerValues) {
            val rlp = bigInteger.toRLP()
            val rlpBytes = rlp.encode()
            LOG.info("bigInteger: $bigInteger ${rlp.bytes.size}")
            val int1 = rlp.toBigIntegerFromRLP()
            Assert.assertEquals(bigInteger, int1)
            val rlpBytes1 = org.ethereum.util.RLP.encodeBigInteger(bigInteger)
            Assert.assertArrayEquals(rlpBytes, rlpBytes1)
            println(Utils.HEX.encode(rlpBytes))
        }
    }

    //for https://github.com/walleth/kethereum/issues/49
    val specialIntegerValues = listOf(0, 9921459, 1838383984)

    @Test
    fun testIntegerRLP() {
        for (value in specialIntegerValues) {
            val rlp = value.toRLP()
            val bytes = rlp.encode()
            LOG.info("integer: $value ${rlp.bytes.size}")
            println(Utils.HEX.encode(bytes))

            val bytes1 = org.ethereum.util.RLP.encodeInt(value)
            Assert.assertArrayEquals(bytes1, bytes)

            val value1 = org.ethereum.util.RLP.decodeInt(bytes1, 0)
            Assert.assertEquals(value, value1)
            println(Utils.HEX.encode(bytes1))

            val value2 = rlp.toIntFromRLP()
            Assert.assertEquals(value, value2)
        }
    }

    val specialStrings = listOf("", "\u0000", "\u0001")

    @Test
    fun testStringRLP() {
        for (value in specialStrings) {
            val rlp = value.toRLP()
            val bytes = rlp.encode()
            LOG.info("string: $value ${rlp.bytes.size}")
            println(Utils.HEX.encode(bytes))

            val bytes1 = org.ethereum.util.RLP.encodeString(value)
            Assert.assertArrayEquals(bytes, bytes1)
            val result = org.ethereum.util.RLP.decode(bytes1, 0).decoded
            val value1 = when (result) {
                is String -> result
                is ByteArray -> String(result)
                else -> RuntimeException("unsupported type: ${result.javaClass}")
            }
            Assert.assertEquals(value, value1)
            println(Utils.HEX.encode(bytes1))

            val value2 = rlp.toStringFromRLP()
            Assert.assertEquals(value, value2)

            val rlp1 = bytes.decodeRLP()
            Assert.assertEquals(rlp, rlp1)
        }
    }

    @Test
    fun testLongRLP() {
        val long = 54408193066555392L
        val rlp = long.toRLP()
        val long1 = rlp.toLongFromRLP()
        Assert.assertEquals(long, long1)
    }

    @Test
    fun testRLP() {
        val data = TestData.random()
        val namedData = NamedData("test", data)
        val bytes = RLP.dump(namedData)
        val namedData1 = RLP.load<NamedData>(bytes)
        Assert.assertEquals(namedData, namedData1)
    }

    //TODO test
    @Ignore
    @Test
    fun testOptionalObject() {
        val namedData = NamedData("test", null)
        val bytes = RLP.dump(namedData)
        val namedData1 = RLP.load<NamedData>(bytes)
        Assert.assertEquals(namedData, namedData1)
    }

    /**
     *  RLP can not distinguish empty string and null string.
     */
    //TODO test
    @Ignore
    @Test
    fun testOptionalString() {
        val data = TestData.random()
        data.optionalValue = ""
        val bytes = RLP.dump(data)
        val data1 = RLP.load<TestData>(bytes)
        Assert.assertEquals(data, data1)
    }


    @Test
    fun testCollection() {
        val data = TestCollectionData("test", listOf(TestData.mock()))
        val bytes = RLP.dump(TestCollectionData.serializer(), data)
        val data2 = RLP.load(TestCollectionData.serializer(), bytes)
        Assert.assertEquals(data, data2)
    }
}
