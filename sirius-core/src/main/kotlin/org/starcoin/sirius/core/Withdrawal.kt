package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils

@ProtobufSchema(Starcoin.InitiateWithdrawalRequest::class)
@Serializable
data class Withdrawal(
    @SerialId(1)
    var address: Address = Address.DUMMY_ADDRESS,
    @SerialId(2)
    var path: AMTreePath = AMTreePath.DUMMY_PATH,
    @SerialId(3)
    var amount: Long = 0
) : SiriusObject() {

    companion object : SiriusObjectCompanion<Withdrawal, Starcoin.InitiateWithdrawalRequest>(Withdrawal::class) {

        var DUMMY_WITHDRAWAL = Withdrawal()

        override fun mock(): Withdrawal {
            return Withdrawal(Address.random(), AMTreePath.mock(), MockUtils.nextLong())
        }
    }
}
