package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.CancelWithdrawalRequest::class)
@Serializable
data class CancelWithdrawal(
    @SerialId(1)
    var participant: Participant = Participant.DUMMY_PARTICIPANT,
    @SerialId(2)
    var update: Update = Update.DUMMY_UPDATE,
    @SerialId(3)
    var path: AMTreePath = AMTreePath.DUMMY_PATH
) : SiriusObject() {

    companion object :
        SiriusObjectCompanion<CancelWithdrawal, Starcoin.CancelWithdrawalRequest>(CancelWithdrawal::class) {

        var DUMMY_CANCEL_WITHDRAWAL = CancelWithdrawal()

        override fun mock(): CancelWithdrawal {
            return CancelWithdrawal(Participant.mock(), Update.mock(), AMTreePath.mock())
        }
    }
}
