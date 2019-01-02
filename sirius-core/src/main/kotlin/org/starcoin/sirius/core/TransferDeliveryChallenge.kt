package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.TransferDeliveryChallenge::class)
@Serializable
data class TransferDeliveryChallenge(
    @SerialId(1)
    val update: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    val tx: OffchainTransaction = OffchainTransaction.DUMMY_OFFCHAIN_TRAN,
    @SerialId(3)
    val path: MerklePath = MerklePath.DUMMY_PATH
) : SiriusObject() {
    companion object : SiriusObjectCompanion<TransferDeliveryChallenge, Starcoin.TransferDeliveryChallenge>(
        TransferDeliveryChallenge::class
    ) {

        var DUMMY_TRAN_DELIVERY_CHALLENGE = TransferDeliveryChallenge()

        override fun mock(): TransferDeliveryChallenge {
            return TransferDeliveryChallenge(Update.mock(), OffchainTransaction.mock(), MerklePath.mock())
        }
    }
}
