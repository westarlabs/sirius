package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin.ProtoHubInfo
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import org.starcoin.sirius.util.MockUtils
import java.security.PublicKey

@ProtobufSchema(ProtoHubInfo::class)
@Serializable
data class HubInfo(
    @SerialId(1)
    val isReady: Boolean = false,
    @SerialId(2)
    val eon: Int = 0,
    @SerialId(3)
    val blocksPerEon: Int = 4,
    @SerialId(4)
    val root: AMTreePathInternalNode = AMTreePathInternalNode.DUMMY_NODE,
    @SerialId(5)
    @Serializable(with = PublicKeySerializer::class)
    val publicKey: PublicKey = CryptoService.getDummyCryptoKey().getKeyPair().public
) : SiriusObject() {

    companion object : SiriusObjectCompanion<HubInfo, ProtoHubInfo>(HubInfo::class) {
        override fun mock(): HubInfo {
            return HubInfo(
                MockUtils.nextBoolean(),
                MockUtils.nextInt(10, 100) * 4,
                MockUtils.nextInt(),
                AMTreePathInternalNode.mock(),
                CryptoService.generateCryptoKey().getKeyPair().public
            )
        }

    }
}
