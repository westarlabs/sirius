package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.starcoin.sirius.serialization.NamedData
import org.starcoin.sirius.serialization.TestData

@Serializable
data class TestCollectionData(val name: String, val list: List<TestData>)

@ImplicitReflectionSerializer
class RLPTest {

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

    //TODO
    //@Test
    fun testObjectNestMany() {
        0.rangeTo(1000).forEach {
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

    /**
     * for https://github.com/walleth/kethereum/issues/49
     */
    @Test
    fun testIntRLP() {
        val int = 1838383984
        val rlp = int.toRLP()
        println(rlp.bytes.size)
        val int1 = rlp.toIntFromRLP()
        Assert.assertEquals(int, int1)
    }

//    @Test
//    fun testLongRLP(){
//        val long = 54408193066555392L
//        val rlp = long.toRLP()
//        val long1 = rlp.toLongFromRLP()
//        Assert.assertEquals(long,long1)
//    }

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
