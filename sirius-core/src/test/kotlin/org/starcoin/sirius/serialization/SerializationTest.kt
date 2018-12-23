package org.starcoin.sirius.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.protobuf.ProtoBuf


class SerializationTest {

    @ImplicitReflectionSerializer
    @Test
    fun testDataClass() {
        val data = TestData.random()
        val bytes = ProtoBuf.dump(data)
        val data1 = ProtoBuf.load<TestData>(bytes)
        Assert.assertEquals(data, data1)

        val json = JSON.stringify(data)
        println(json)
        val data2 = JSON.parse<TestData>(json);
        Assert.assertEquals(data, data2)
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


//    @Test
//    fun testTransform() {
//        //TODO type converter
//        val from = BlockAddress.random()
//        val to = BlockAddress.random()
//
//        val tx = OffchainTransaction(0, from, to, RandomUtils.nextLong())
//        tx.sign(KeyPairUtil.TEST_KEYPAIR.private)
//        var proto = Transform.transform<Starcoin.ProtoOffchainTransaction, OffchainTransaction>(tx)
//
//        val tx1 = OffchainTransaction.genarateHubTransaction(proto!!)
//
//        Assert.assertEquals(tx1, tx)
//    }
}
