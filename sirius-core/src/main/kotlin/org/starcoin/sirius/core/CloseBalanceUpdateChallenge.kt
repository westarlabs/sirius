package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.CloseBalanceUpdateChallenge::class)
@Serializable
data class CloseBalanceUpdateChallenge(
    @SerialId(1)
    val update: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<CloseBalanceUpdateChallenge, Starcoin.ProtoBalanceUpdateChallenge>(
            CloseBalanceUpdateChallenge::class
        ) {

        var DUMMY_BALANCE_UPDATE_PROOF = CloseBalanceUpdateChallenge(Update.DUMMY_UPDATE, AMTreeProof.DUMMY_PROOF)

        override fun mock(): CloseBalanceUpdateChallenge {
            return CloseBalanceUpdateChallenge(Update.mock(), AMTreeProof.mock())
        }
    }
}
