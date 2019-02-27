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
        const val TEST_BLOCKS_PER_EON = 16
        val DEFAULT_ARG = ContractConstructArgs(TEST_BLOCKS_PER_EON, HubRoot.EMPTY_TREE_HUBROOT)
        override fun mock(): ContractConstructArgs {
            return ContractConstructArgs(
                TEST_BLOCKS_PER_EON,
                HubRoot.mock()
            )
        }
    }
}

fun main() {
    println(ContractConstructArgs.DEFAULT_ARG.toRLP().toHEXString())
}