package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.RecoverFundsRequest::class)
@Serializable
data class RecoverFunds(
    @SerialId(1)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF,
    @SerialId(2)
    val address: Address = Address.DUMMY_ADDRESS
) : SiriusObject() {
    companion object : SiriusObjectCompanion<RecoverFunds, Starcoin.RecoverFundsRequest>(RecoverFunds::class) {

        var DUMMY_RECOVER_FUND = RecoverFunds()

        override fun mock(): RecoverFunds {
            return RecoverFunds(AMTreeProof.mock(), Address.DUMMY_ADDRESS)
        }
    }
}
