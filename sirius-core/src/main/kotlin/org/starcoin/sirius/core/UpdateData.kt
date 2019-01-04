package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger

@Serializable
@ProtobufSchema(Starcoin.UpdateData::class)
data class UpdateData(
    @SerialId(1)
    var eon: Int = 0,
    @SerialId(2)
    var version: Long = 0,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    var sendAmount: BigInteger = BigInteger.ZERO,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    var receiveAmount: BigInteger = BigInteger.ZERO,
    @SerialId(5)
    var root: Hash = Hash.EMPTY_DADA_HASH
) : SiriusObject() {

    constructor(
        eon: Int,
        version: Long,
        sendAmount: Long,
        receiveAmount: Long,
        root: Hash = Hash.EMPTY_DADA_HASH
    ) : this(eon, version, sendAmount.toBigInteger(), receiveAmount.toBigInteger(), root)

    companion object : SiriusObjectCompanion<UpdateData, Starcoin.UpdateData>(
        UpdateData::class
    ) {

        var DUMMY_UPDATE_DATA = UpdateData()

        override fun mock(): UpdateData {
            return UpdateData(
                MockUtils.nextInt(),
                MockUtils.nextLong(),
                MockUtils.nextBigInteger(),
                MockUtils.nextBigInteger(),
                Hash.random()
            )
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): UpdateData {
            val tree = MerkleTree(txs)
            val root = tree.hash()
            val sendAmount = txs.stream()
                .filter { transaction -> transaction.from == address }
                .map<BigInteger> { it.amount }
                .reduce(BigInteger.ZERO) { a, b -> a.add(b) }
            val receiveAmount = txs.stream()
                .filter { transaction -> transaction.to == address }
                .map<BigInteger> { it.amount }
                .reduce(BigInteger.ZERO) { a, b -> a.add(b) }
            return UpdateData(eon, version, sendAmount, receiveAmount, root)
        }

        override fun parseFromProtoMessage(proto: Starcoin.UpdateData): UpdateData {
            return UpdateData(
                proto.eon,
                proto.version,
                BigInteger(proto.sendAmount.toByteArray()),
                BigInteger(proto.receiveAmount.toByteArray()),
                Hash.wrap(proto.root)
            )
        }

        override fun toProtoMessage(obj: UpdateData): Starcoin.UpdateData {
            return Starcoin.UpdateData.newBuilder()
                .setEon(obj.eon)
                .setVersion(obj.version)
                .setSendAmount(obj.sendAmount.toByteArray().toByteString())
                .setReceiveAmount(obj.receiveAmount.toByteArray().toByteString())
                .setRoot(obj.root.toByteString()).build()
        }
    }
}
