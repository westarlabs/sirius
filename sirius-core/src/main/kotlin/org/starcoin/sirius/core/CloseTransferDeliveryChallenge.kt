package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.CloseTransferDeliveryChallengeRequest::class)
@Serializable
data class CloseTransferDeliveryChallenge(
    @SerialId(1)
    var merklePath: AMTPath = AMTPath.DUMMY_PATH,
    @SerialId(2)
    var update: Update = Update.DUMMY_UPDATE,
    @SerialId(3)
    var path: MerklePath = MerklePath(),
    @SerialId(4)
    var fromPublicKey: Participant = Participant.DUMMY_PARTICIPANT
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<CloseTransferDeliveryChallenge, Starcoin.CloseTransferDeliveryChallengeRequest>(
            CloseTransferDeliveryChallenge::class
        ) {

        var DUMMY_CLOSE_TRAN_DELIVERY_CHALLENGE = CloseTransferDeliveryChallenge()

        override fun mock(): CloseTransferDeliveryChallenge {
            return CloseTransferDeliveryChallenge(
                AMTPath.mock(),
                Update.mock(),
                MerklePath.mock(),
                Participant.mock()
            )
        }
    }
}
