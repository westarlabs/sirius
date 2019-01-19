package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import org.starcoin.sirius.util.MockUtils
import java.security.PublicKey

@ProtobufSchema(Starcoin.HubInfo::class)
@Serializable
data class HubInfo(
    @SerialId(1)
    val isReady: Boolean = false,
    @SerialId(2)
    val eon: Int = 0,
    @SerialId(3)
    val blocksPerEon: Int = 4,
    @SerialId(4)
    val root: AMTreePathNode = AMTreePathNode.DUMMY_NODE,
    @SerialId(5)
    @Serializable(with = PublicKeySerializer::class)
    val publicKey: PublicKey = CryptoService.dummyCryptoKey.keyPair.public
) : SiriusObject() {

    companion object : SiriusObjectCompanion<HubInfo, Starcoin.HubInfo>(HubInfo::class) {
        override fun mock(): HubInfo {
            return HubInfo(
                MockUtils.nextBoolean(),
                MockUtils.nextInt(10, 100) * 4,
                MockUtils.nextInt(),
                AMTreePathNode.mock(),
                CryptoService.generateCryptoKey().keyPair.public
            )
        }

    }
}
