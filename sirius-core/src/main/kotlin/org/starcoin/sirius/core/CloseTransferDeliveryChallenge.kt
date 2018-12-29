package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import java.security.PublicKey

@ProtobufSchema(Starcoin.CloseTransferDeliveryChallengeRequest::class)
@Serializable
data class CloseTransferDeliveryChallenge(
    @SerialId(1)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF,
    @SerialId(2)
    val update: Update = Update.DUMMY_UPDATE,
    @SerialId(3)
    val txPath: MerklePath = MerklePath.DUMMY_PATH,
    @SerialId(4)
    @Serializable(with = PublicKeySerializer::class)
    val fromPublicKey: PublicKey = CryptoService.dummyCryptoKey.keyPair.public
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<CloseTransferDeliveryChallenge, Starcoin.CloseTransferDeliveryChallengeRequest>(
            CloseTransferDeliveryChallenge::class
        ) {

        var DUMMY_CLOSE_TRAN_DELIVERY_CHALLENGE = CloseTransferDeliveryChallenge()

        override fun mock(): CloseTransferDeliveryChallenge {
            return CloseTransferDeliveryChallenge(
                AMTreeProof.mock(),
                Update.mock(),
                MerklePath.mock(),
                CryptoService.dummyCryptoKey.keyPair.public
            )
        }
    }
}
