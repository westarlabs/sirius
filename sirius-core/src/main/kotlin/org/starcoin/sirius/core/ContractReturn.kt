package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoContractReturn::class)
@Serializable
data class ContractReturn(@SerialId(1) val hasVal: Boolean, @SerialId(2) val payload: ByteArrayWrapper) :
    SiriusObject() {

    companion object : SiriusObjectCompanion<ContractReturn, Starcoin.ProtoContractReturn>(ContractReturn::class) {
        override fun mock(): ContractReturn {
            return ContractReturn(true, ByteArrayWrapper(BalanceUpdateChallenge.mock().toRLP()))
        }
    }
}
