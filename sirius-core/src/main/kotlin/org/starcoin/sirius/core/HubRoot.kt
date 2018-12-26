package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin

//@ProtobufSchema(Starcoin.ProtoHubRoot::class)
//@Serializable
data class HubRoot(
    @SerialId(1)
    var root: AugmentedMerkleTree.AugmentedMerkleTreeNode = AugmentedMerkleTree.AugmentedMerkleTreeNode(0),
    @SerialId(2)
    var eon: Long = 0
) : SiriusObject() {
    companion object : SiriusObjectCompanion<HubRoot, Starcoin.ProtoHubRoot>(HubRoot::class) {
        override fun mock(): HubRoot {
            var node = NodeInformation.mock()
            return HubRoot(
                AugmentedMerkleTree.AugmentedMerkleTreeNode(
                    node.offset,
                    node,
                    RandomUtils.nextLong()
                ), RandomUtils.nextLong()
            )
        }
    }
}
