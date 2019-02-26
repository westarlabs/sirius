package org.starcoin.sirius.protocol

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.core.SiriusObjectCompanion
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.ProtobufSchema

@Serializable
@ProtobufSchema(Starcoin.ContractConstructArgs::class)
data class ContractConstructArgs(@SerialId(1) val blocksPerEon: Int, @SerialId(2) val hubRoot: HubRoot) :
    SiriusObject() {
    companion object :
        SiriusObjectCompanion<ContractConstructArgs, Starcoin.ContractConstructArgs>(
            ContractConstructArgs::class
        ) {
        val DEFAULT_ARG = ContractConstructArgs(8, HubRoot.EMPTY_TREE_HUBROOT)
        //0xe808e6e4a02066cbab68b2637c42f33fdd66c8085ece5bff04e319e1c95e5e6be2457d887580808080
        override fun mock(): ContractConstructArgs {
            return ContractConstructArgs(
                8,
                HubRoot.mock()
            )
        }
    }
}

fun main() {
    println(ContractConstructArgs.DEFAULT_ARG.toRLP().toHEXString())
}