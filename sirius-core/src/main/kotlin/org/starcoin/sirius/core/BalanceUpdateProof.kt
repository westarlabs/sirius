package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.BalanceUpdateProof::class)
@Serializable
data class BalanceUpdateProof(
    @SerialId(1)
    val hasUp: Boolean = false,
    @SerialId(2)
    val update: Update = Update.DUMMY_UPDATE,
    @SerialId(3)
    val hasPath: Boolean = false,
    @SerialId(4)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF
) : SiriusObject() {

    constructor(update: Update?, proof: AMTreeProof?) : this(
        update != null,
        update ?: Update.DUMMY_UPDATE,
        proof != null,
        proof ?: AMTreeProof.DUMMY_PROOF
    )

    constructor(update: Update) : this(update, null)
    constructor(proof: AMTreeProof) : this(null, proof)

    companion object :
        SiriusObjectCompanion<BalanceUpdateProof, Starcoin.BalanceUpdateChallenge>(BalanceUpdateProof::class) {

        var DUMMY_BALANCE_UPDATE_PROOF = BalanceUpdateProof(false, Update.DUMMY_UPDATE, false, AMTreeProof.DUMMY_PROOF)

        override fun mock(): BalanceUpdateProof {
            return BalanceUpdateProof(false, Update.mock(), false, AMTreeProof.mock())
        }
    }
}
