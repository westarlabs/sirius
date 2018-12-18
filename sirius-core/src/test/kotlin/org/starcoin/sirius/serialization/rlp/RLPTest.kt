package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.serialization.BinaryElementValueDecoder
import org.starcoin.sirius.serialization.BinaryElementValueEncoder
import org.starcoin.sirius.util.Utils

@Serializable
data class ByteArrayWrapper(val byteArray: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayWrapper) return false

        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }

    @Serializer(forClass = ByteArrayWrapper::class)
    companion object : KSerializer<ByteArrayWrapper> {

        override fun deserialize(input: Decoder): ByteArrayWrapper {
            return when (input) {
                is BinaryElementValueDecoder -> ByteArrayWrapper(input.decodeByteArray())
                else -> ByteArrayWrapper(Utils.HEX.decode(input.decodeString()))
            }
        }

        override fun serialize(output: Encoder, obj: ByteArrayWrapper) {
            when (output) {
                is BinaryElementValueEncoder -> output.encodeByteArray(obj.byteArray)
                else -> output.encodeString(Utils.HEX.encode(obj.byteArray))
            }
        }
    }
}

@Serializable
data class Data(
    val boolean: Boolean,
    val byte: Byte,
    val int: Int,
    val long: Long,
    val string: String,
    val byteArray: ByteArrayWrapper
) {

    companion object {
        fun random(): Data {
            return Data(
                RandomUtils.nextBoolean(),
                RandomUtils.nextInt(0, Byte.MAX_VALUE.toInt()).toByte(),
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 30)),
                ByteArrayWrapper(RandomUtils.nextBytes(RandomUtils.nextInt(10, 100)))
            )
        }
    }

}

@Serializable
data class NamedData(val name: String, val data: Data)

@ImplicitReflectionSerializer
class RLPTest {

    @Test
    fun testRLPInputOutput() {
        val data = Data.random()

        var rlpList = RLPList(mutableListOf())
        val output = RLPOutput(rlpList, true)
        output.encode(data)

        Assert.assertTrue(rlpList.element.isNotEmpty())

        var bytes = rlpList.encode()
        var rlpList1 = bytes.decodeRLP() as RLPList

        Assert.assertEquals(rlpList.element.size, rlpList1.element.size)

        val input = RLPInput(rlpList1.element.iterator(), true)
        val data1 = input.decode(Data.serializer())
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
        val data = Data.random()
        val namedData = NamedData("test", data)
        val jsonString = JSON.stringify(namedData)
        //println(jsonString)
        val namedData1 = JSON.parse<NamedData>(jsonString)
        Assert.assertEquals(namedData, namedData1)
    }

    @Test
    fun testObjectNest() {
        val data = Data.random()
        val namedData = NamedData("test", data)
        var rlpList = RLPList(mutableListOf())
        val output = RLPOutput(rlpList, true)
        output.encode(namedData)

        Assert.assertTrue(rlpList.element.isNotEmpty())

        var bytes = rlpList.encode()
        var rlpList1 = bytes.decodeRLP() as RLPList

        Assert.assertEquals(rlpList.element.size, rlpList1.element.size)

        val input = RLPInput(rlpList1.element.iterator(), true)
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
        val data = Data.random()
        val namedData = NamedData("test", data)
        val bytes = RLP.dump(namedData)
        val namedData1 = RLP.load<NamedData>(bytes)
        Assert.assertEquals(namedData, namedData1)
    }
}