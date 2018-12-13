package org.starcoin.sirius.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.OffchainTransaction
import org.starcoin.sirius.util.KeyPairUtil
import org.starcoin.sirius.util.Utils

@Serializable
data class Data(val name: String, val age: Int, val bytes: DataBytes)

@Serializable
data class DataBytes private constructor(val bytes: ByteArray) {


    @Serializer(forClass = DataBytes::class)
    companion object : KSerializer<DataBytes> {

        override fun serialize(output: Encoder, obj: DataBytes) {
            output.encodeString(Utils.HEX.encode(obj.bytes))
        }

        override fun deserialize(input: Decoder): DataBytes {
            return DataBytes(Utils.HEX.decode(input.decodeString()))
        }

        fun valueOf(bytes: ByteArray): DataBytes {
            return DataBytes(bytes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataBytes) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.bytes)
    }
}


class SerializationTest {

    @ImplicitReflectionSerializer
    @Test
    fun testDataClass() {
        val data = Data("aaa", 10, DataBytes.valueOf("bbb".toByteArray()))
        val bytes = ProtoBuf.dump(data)
        val data1 = ProtoBuf.load<Data>(bytes)
        Assert.assertEquals(data, data1)

        val json = JSON.stringify(data)
        println(json)
        val data2 = JSON.parse<Data>(json);
        Assert.assertEquals(data, data2)
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