package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoBalanceUpdateProof::class)
data class BalanceUpdateProof(
    @SerialId(1)
    var update: Update = Update.DUMMY_UPDATE,
    @SerialId(2)
    var provePath: AugmentedMerklePath
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<BalanceUpdateProof, Starcoin.ProtoBalanceUpdateChallenge>(BalanceUpdateProof::class) {

        var DUMMY_BALANCE_UPDATE_PROOF = BalanceUpdateProof(Update.DUMMY_UPDATE, AugmentedMerklePath())

        override fun mock(): BalanceUpdateProof {
            return BalanceUpdateProof(Update.mock(), AugmentedMerklePath())
        }
    }
}
