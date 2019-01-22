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
    val update: Update? = Update.DUMMY_UPDATE,
    @SerialId(3)
    val hasPath: Boolean = false,
    @SerialId(4)
    val path: AMTreePath? = AMTreePath.DUMMY_PATH
) : SiriusObject() {

    constructor(proof: AMTreeProof) : this(
        true, proof.leaf.update, true, proof.path
    )

    constructor(update: Update) : this(true, update, false, null)
    constructor(path: AMTreePath) : this(false, null, true, path)

    companion object :
        SiriusObjectCompanion<BalanceUpdateProof, Starcoin.BalanceUpdateProof>(BalanceUpdateProof::class) {

        var DUMMY_BALANCE_UPDATE_PROOF = BalanceUpdateProof(false, Update.DUMMY_UPDATE, false, AMTreePath.DUMMY_PATH)

        override fun mock(): BalanceUpdateProof {
            return BalanceUpdateProof(true, Update.mock(), true, AMTreePath.mock())
        }
    }
}
