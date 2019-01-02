package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.OpenTransferDeliveryChallenge::class)
@Serializable
data class OpenTransferDeliveryChallenge(
    @SerialId(1)
    val updata: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    val tx: OffchainTransaction = OffchainTransaction.DUMMY_OFFCHAIN_TRAN,
    @SerialId(3)
    val path: MerklePath = MerklePath.DUMMY_PATH
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<OpenTransferDeliveryChallenge, Starcoin.OpenTransferDeliveryChallenge>(
            OpenTransferDeliveryChallenge::class
        ) {

        var DUMMY_OPEN_TRAN_DELIVERY_CHALLENGE = OpenTransferDeliveryChallenge()

        override fun mock(): OpenTransferDeliveryChallenge {
            return OpenTransferDeliveryChallenge(
                Update.mock(),
                OffchainTransaction.mock(),
                MerklePath.mock()
            )
        }
    }
}
