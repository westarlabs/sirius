package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger

@ProtobufSchema(Starcoin.ContractHubInfo::class)
@Serializable
data class ContractHubInfo(
    @SerialId(1)
    @Serializable(with = BigIntegerSerializer::class)
    val startBlockNumber: BigInteger,
    @SerialId(2)
    val hubAddress: String,
    @SerialId(3)
    val blocksPerEon: Int,
    @SerialId(4)
    val eon: Int = 0
) : SiriusObject() {
    companion object : SiriusObjectCompanion<ContractHubInfo, Starcoin.ContractHubInfo>(ContractHubInfo::class) {
        override fun mock(): ContractHubInfo {
            return ContractHubInfo(MockUtils.nextBigInteger(), "127.0.0.1:8990", 4, MockUtils.nextInt())
        }
    }
}
