package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger

//TODO ensure timestamp
@Serializable
@ProtobufSchema(Starcoin.OffchainTransactionData::class)
data class OffchainTransactionData(
    @SerialId(1)
    val eon: Int = 0,
    @SerialId(2)
    val from: Address,
    @SerialId(3)
    val to: Address,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger = BigInteger.ZERO,
    @SerialId(5)
    val timestamp: Long = System.currentTimeMillis()
) : SiriusObject() {

    constructor(
        eon: Int,
        from: Address,
        to: Address,
        amount: Long,
        timestamp: Long = System.currentTimeMillis()
    ) : this(eon, from, to, amount.toBigInteger(), timestamp)

    companion object :
        SiriusObjectCompanion<OffchainTransactionData, Starcoin.OffchainTransactionData>(
            OffchainTransactionData::class
        ) {

        var DUMMY_OFFCHAIN_TRAN_DATA =
            OffchainTransactionData(0, Address.DUMMY_ADDRESS, Address.DUMMY_ADDRESS, BigInteger.ZERO, 0)

        override fun mock(): OffchainTransactionData {
            return OffchainTransactionData(
                MockUtils.nextInt(),
                Address.random(),
                Address.random(),
                MockUtils.nextBigInteger(),
                MockUtils.nextLong()
            )
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.OffchainTransactionData): OffchainTransactionData {
            return OffchainTransactionData(
                protoMessage.eon,
                Address.wrap(protoMessage.from),
                Address.wrap(protoMessage.to),
                BigInteger(protoMessage.amount.toByteArray()),
                protoMessage.timestamp
            )
        }

        override fun toProtoMessage(obj: OffchainTransactionData): Starcoin.OffchainTransactionData {
            return Starcoin.OffchainTransactionData.newBuilder()
                .setEon(obj.eon)
                .setFrom(obj.from.toByteString())
                .setTo(obj.to.toByteString())
                .setAmount(obj.amount.toByteArray().toByteString())
                .setTimestamp(obj.timestamp).build()
        }
    }
}
