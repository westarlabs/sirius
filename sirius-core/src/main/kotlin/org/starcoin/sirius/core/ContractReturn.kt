package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.serialization.ProtobufSchema
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

@ProtobufSchema(Starcoin.ProtoContractReturn::class)
@Serializable
data class ContractReturn(@SerialId(1) val hasVal: Boolean, @SerialId(2) val payload: ByteArrayWrapper) :
    SiriusObject() {


    fun <S : SiriusObject> getPayload(clazz: KClass<S>): S? {
        return if (hasVal) (clazz.companionObjectInstance as SiriusObjectCompanion<S, *>).parseFromRLP(this.payload.bytes) else null
    }

    companion object : SiriusObjectCompanion<ContractReturn, Starcoin.ProtoContractReturn>(ContractReturn::class) {
        override fun mock(): ContractReturn {
            return ContractReturn(true, ByteArrayWrapper(BalanceUpdateProof.mock().toRLP()))
        }
    }
}
