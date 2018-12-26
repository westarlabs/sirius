package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoHubRoot::class)
@Serializable
data class HubRoot(
    @SerialId(1)
    val root: AMTreePathInternalNode = AMTreePathInternalNode.DUMMY_NODE,
    @SerialId(2)
    val eon: Int = 0
) : SiriusObject() {
    companion object : SiriusObjectCompanion<HubRoot, Starcoin.ProtoHubRoot>(HubRoot::class) {

        var DUMMY_HUB_ROOT = HubRoot()

        override fun mock(): HubRoot {
            var node = NodeInformation.mock()
            return HubRoot(
                AMTreePathInternalNode.mock(), RandomUtils.nextInt()
            )
        }
    }
}
