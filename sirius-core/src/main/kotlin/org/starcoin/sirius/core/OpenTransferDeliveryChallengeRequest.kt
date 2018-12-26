package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.OpenTransferDeliveryChallengeRequest::class)
@Serializable
data class OpenTransferDeliveryChallengeRequest(
    @SerialId(1)
    var updata: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    var tran: OffchainTransaction = OffchainTransaction.DUMMY_OFFCHAIN_TRAN,
    @SerialId(3)
    var path: AMTPath = AMTPath.DUMMY_PATH
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<OpenTransferDeliveryChallengeRequest, Starcoin.OpenTransferDeliveryChallengeRequest>(
            OpenTransferDeliveryChallengeRequest::class
        ) {

        var DUMMY_OPEN_TRAN_DELIVERY_CHALLENGE = OpenTransferDeliveryChallengeRequest()

        override fun mock(): OpenTransferDeliveryChallengeRequest {
            return OpenTransferDeliveryChallengeRequest(
                Update.mock(),
                OffchainTransaction.mock(),
                AMTPath.mock()
            )
        }
    }
}
