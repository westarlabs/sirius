package org.starcoin.sirius.protocol

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.core.SiriusObjectCompanion
import org.starcoin.sirius.serialization.ProtobufSchema

@Serializable
@ProtobufSchema(Starcoin.ContractConstructArgs::class)
data class ContractConstructArgs(@SerialId(1) val blocks: Long, @SerialId(2) val hubRoot: HubRoot) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<ContractConstructArgs, Starcoin.ContractConstructArgs>(
            ContractConstructArgs::class
        ) {
        val DEFAULT_ARG = ContractConstructArgs(8, HubRoot.EMPTY_TREE_HUBROOT)
        override fun mock(): ContractConstructArgs {
            return ContractConstructArgs(
                8,
                HubRoot.mock()
            )
        }
    }
}
