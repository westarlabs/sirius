package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoContractReturn::class)
@Serializable
data class ContractReturn(@SerialId(1) val hasVal: Boolean, @SerialId(2) val type: ContractReturnType, @SerialId(3) val payload: ByteArrayWrapper) :
    SiriusObject() {

    constructor(hasVal: Boolean, type: ContractReturnType, obj: SiriusObject) : this(
        hasVal,
        type,
        ByteArrayWrapper(obj.toProtobuf())
    )

    fun <D : SiriusObject> getPayload(): D? {
        when (hasVal) {
            false -> return null
            true -> return type.parsePayload(payload.bytes)
        }
    }

    companion object : SiriusObjectCompanion<ContractReturn, Starcoin.ProtoContractReturn>(ContractReturn::class) {
        override fun mock(): ContractReturn {
            val type = ContractReturnType.random()
            return when (type) {
                ContractReturnType.CR_WITHDRAWAL -> ContractReturn(true, type, Withdrawal.mock())
                ContractReturnType.CR_BALANCE -> {
                    ContractReturn(true, type, BalanceUpdateChallenge.mock())
                }
                ContractReturnType.CR_TRANSFER -> {
                    ContractReturn(true, type, TransferDeliveryChallenge.mock())
                }
                ContractReturnType.CR_HUBROOT -> {
                    ContractReturn(true, type, HubRoot.mock())
                }
            }
        }
    }
}