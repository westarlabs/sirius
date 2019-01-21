package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger

@ProtobufSchema(Starcoin.Withdrawal::class)
@Serializable
data class Withdrawal(
    @SerialId(1)
    val proof:AMTreeProof = AMTreeProof.DUMMY_PROOF,
    @SerialId(2)
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger = BigInteger.ZERO
) : SiriusObject() {

    constructor(proof: AMTreeProof, amount: Long) : this(proof, amount.toBigInteger())

    companion object : SiriusObjectCompanion<Withdrawal, Starcoin.Withdrawal>(Withdrawal::class) {

        var DUMMY_WITHDRAWAL = Withdrawal()

        override fun mock(): Withdrawal {
            return Withdrawal(AMTreeProof.mock(), MockUtils.nextBigInteger())
        }
    }
}
