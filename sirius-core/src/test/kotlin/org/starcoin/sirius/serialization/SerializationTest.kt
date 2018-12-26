package org.starcoin.sirius.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.protobuf.ProtoBuf
import org.starcoin.sirius.serialization.rlp.RLP


class SerializationTest {

    @ImplicitReflectionSerializer
    @Test
    fun testDataClass() {
        val data = TestData.random()

        val json = JSON.stringify(data)
        println(json)
        val data1 = JSON.parse<TestData>(json)
        Assert.assertEquals(data, data1)

        val bytes = ProtoBuf.dump(data)
        val data2 = ProtoBuf.load<TestData>(bytes)
        Assert.assertEquals(data, data2)

        val rlp = RLP.dump(data)
        val data3 = RLP.load<TestData>(rlp)
        Assert.assertEquals(data, data3)
    }

    @ImplicitReflectionSerializer
    @Test
    fun testNamedData() {
        val data = TestData.random()
        println(data.toJSON())
        val namedData = NamedData("test", data)
        val json = JSON.stringify(NamedData.serializer(), namedData)
        println(json)
        val namedData1 = JSON.parse(NamedData.serializer(), json)
        Assert.assertEquals(namedData, namedData1)
    }

    @ImplicitReflectionSerializer
    @Test
    fun testProtoMessage(){
        testForDefaultValue(false, "")
        testForDefaultValue(true, "aaa")
    }

    fun testForDefaultValue(booleanValue:Boolean, stringValue:String){
        val data = TestData.random()
        data.booleanValue = booleanValue
        data.stringValue = stringValue
        val bytes = ProtoBuf.dump(TestData.serializer(),data)

        val protoData = Starcoin.TestData.parseFrom(bytes)
        val bytes1 = protoData.toByteArray()
        Assert.assertArrayEquals(bytes, bytes1)

        val data1 = ProtoBuf.load(TestData.serializer(), bytes1)
        Assert.assertEquals(data, data1)
    }

    //TODO
    @ImplicitReflectionSerializer
    @Ignore
    @Test
    fun testOptionalObject() {
        val namedData = NamedData("test", null)

        val json = JSON.stringify(namedData)
        println(json)
        val namedData1 = JSON.parse<NamedData>(json)
        Assert.assertEquals(namedData, namedData1)

        val pb = ProtoBuf.dump(namedData)
        val namedData2 = ProtoBuf.load<NamedData>(pb)
        Assert.assertEquals(namedData, namedData2)

        val rlp = RLP.dump(namedData)
        val namedData3 = RLP.load<NamedData>(rlp)
        Assert.assertEquals(namedData, namedData3)
    }

    @Test
    fun testProtobufSchema() {
        val data = TestData.random()
        val protoData = TestData.toProtoMessage(data)
        Assert.assertArrayEquals(data.toProtobuf(), protoData.toByteArray())
    }

//    @Test
//    fun testTransform() {
//        //TODO type converter
//        val from = BlockAddress.random()
//        val to = BlockAddress.random()
//
//        val tx = OffchainTransaction(0, from, to, MockUtils.nextLong())
//        tx.sign(KeyPairUtil.TEST_KEYPAIR.private)
//        var proto = Transform.transform<Starcoin.ProtoOffchainTransaction, OffchainTransaction>(tx)
//
//        val tx1 = OffchainTransaction.genarateHubTransaction(proto!!)
//
//        Assert.assertEquals(tx1, tx)
//    }
}
