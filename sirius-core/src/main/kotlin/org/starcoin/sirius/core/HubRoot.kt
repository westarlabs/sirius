package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils

@ProtobufSchema(Starcoin.ProtoHubRoot::class)
@Serializable
data class HubRoot(
    @SerialId(1)
    val root: AMTreePathInternalNode = AMTreePathInternalNode.DUMMY_NODE,
    @SerialId(2)
    val eon: Long = 0
) : SiriusObject() {
    companion object : SiriusObjectCompanion<HubRoot, Starcoin.ProtoHubRoot>(HubRoot::class) {

        var DUMMY_HUB_ROOT = HubRoot()

        override fun mock(): HubRoot {
            return HubRoot(
                AMTreePathInternalNode.mock(), MockUtils.nextLong()
            )
        }
    }
}
