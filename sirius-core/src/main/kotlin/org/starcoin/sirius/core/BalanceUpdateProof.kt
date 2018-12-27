package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoBalanceUpdateProof::class)
@Serializable
data class BalanceUpdateProof(
    @SerialId(1)
    val update: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<BalanceUpdateProof, Starcoin.ProtoBalanceUpdateChallenge>(BalanceUpdateProof::class) {

        var DUMMY_BALANCE_UPDATE_PROOF = BalanceUpdateProof(Update.DUMMY_UPDATE, AMTreeProof.DUMMY_PROOF)

        override fun mock(): BalanceUpdateProof {
            return BalanceUpdateProof(Update.mock(), AMTreeProof.mock())
        }
    }
}
