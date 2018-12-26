package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.InitiateWithdrawalRequest::class)
@Serializable
data class Withdrawal(
    @SerialId(1)
    var address: Address = Address.DUMMY_ADDRESS,
    @SerialId(2)
    var path: AugmentedMerklePath = AugmentedMerklePath(),
    @SerialId(3)
    var amount: Long = 0
) : SiriusObject() {

    constructor(init: Starcoin.InitiateWithdrawalRequest) : this(
        Address.wrap(init.address),
        AugmentedMerklePath(init.path),
        init.amount
    )

    companion object : SiriusObjectCompanion<Withdrawal, Starcoin.InitiateWithdrawalRequest>(Withdrawal::class) {

        var DUMMY_WITHDRAWAL = Withdrawal()

        override fun mock(): Withdrawal {
            return Withdrawal(Address.DUMMY_ADDRESS, AugmentedMerklePath(), 0)
        }
    }
}