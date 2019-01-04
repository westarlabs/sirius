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
    val address: Address = Address.DUMMY_ADDRESS,
    @SerialId(2)
    val path: AMTreePath = AMTreePath.DUMMY_PATH,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger = BigInteger.ZERO
) : SiriusObject() {

    constructor(address: Address, path: AMTreePath, amount: Long) : this(address, path, amount.toBigInteger())

    companion object : SiriusObjectCompanion<Withdrawal, Starcoin.Withdrawal>(Withdrawal::class) {

        var DUMMY_WITHDRAWAL = Withdrawal()

        override fun mock(): Withdrawal {
            return Withdrawal(Address.random(), AMTreePath.mock(), MockUtils.nextBigInteger())
        }
    }
}
