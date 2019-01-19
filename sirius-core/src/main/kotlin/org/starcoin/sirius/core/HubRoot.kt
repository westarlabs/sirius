package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils

@ProtobufSchema(Starcoin.HubRoot::class)
@Serializable
data class HubRoot(
    @SerialId(1)
    val root: AMTreePathNode = AMTreePathNode.DUMMY_NODE,
    @SerialId(2)
    val eon: Int = 0
) : SiriusObject() {
    companion object : SiriusObjectCompanion<HubRoot, Starcoin.HubRoot>(HubRoot::class) {

        val DUMMY_HUB_ROOT = HubRoot()
        val EMPTY_TREE_HUBROOT = HubRoot(AMTree().root.toAMTreePathNode(), 0)

        override fun mock(): HubRoot {
            return HubRoot(
                AMTreePathNode.mock(), MockUtils.nextInt()
            )
        }
    }
}
