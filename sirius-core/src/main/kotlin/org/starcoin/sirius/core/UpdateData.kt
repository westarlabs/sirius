package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@Serializable
@ProtobufSchema(Starcoin.UpdateData::class)
data class UpdateData(
    @SerialId(1)
    var eon: Int = 0,
    @SerialId(2)
    var version: Long = 0,
    @SerialId(3)
    var sendAmount: Long = 0,
    @SerialId(4)
    var receiveAmount: Long = 0,
    @SerialId(5)
    var root: Hash = Hash.EMPTY_DADA_HASH
) : SiriusObject() {

    companion object : SiriusObjectCompanion<UpdateData, Starcoin.UpdateData>(
        UpdateData::class
    ) {

        var DUMMY_UPDATE_DATA = UpdateData()

        override fun mock(): UpdateData {
            return UpdateData(
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                Hash.random()
            )
        }

        fun newUpdate(eon: Int, version: Long, address: Address, txs: List<OffchainTransaction>): UpdateData {
            val tree = MerkleTree(txs)
            val root = tree.hash()
            val sendAmount = txs.stream()
                .filter { transaction -> transaction.from == address }
                .map<Long> { it.amount }
                .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
            val receiveAmount = txs.stream()
                .filter { transaction -> transaction.to == address }
                .map<Long> { it.amount }
                .reduce(0L) { a, b -> java.lang.Long.sum(a, b) }
            return UpdateData(eon, version, sendAmount, receiveAmount, root)
        }

        override fun parseFromProtoMessage(proto: Starcoin.UpdateData): UpdateData {
            return UpdateData(
                proto.eon,
                proto.version,
                proto.sendAmount,
                proto.receiveAmount,
                Hash.wrap(proto.root)
            )
        }

        override fun toProtoMessage(obj: UpdateData): Starcoin.UpdateData {
            return Starcoin.UpdateData.newBuilder()
                .setEon(obj.eon)
                .setVersion(obj.version)
                .setSendAmount(obj.sendAmount)
                .setReceiveAmount(obj.receiveAmount)
                .setRoot(obj.root.toByteString()).build()
        }
    }
}
