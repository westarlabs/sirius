package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import java.security.PublicKey

@ProtobufSchema(Starcoin.CloseTransferDeliveryChallenge::class)
@Serializable
data class CloseTransferDeliveryChallenge(
    @SerialId(1)
    val proof: AMTreeProof = AMTreeProof.DUMMY_PROOF,
    @SerialId(2)
    val txPath: MerklePath = MerklePath.DUMMY_PATH,
    @SerialId(3)
    @Serializable(with = PublicKeySerializer::class)
    val fromAddr: Address = Address.DUMMY_ADDRESS,
    @SerialId(4)
    val txHash: Hash = Hash.ZERO_HASH
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<CloseTransferDeliveryChallenge, Starcoin.CloseTransferDeliveryChallenge>(
            CloseTransferDeliveryChallenge::class
        ) {

        var DUMMY_CLOSE_TRAN_DELIVERY_CHALLENGE = CloseTransferDeliveryChallenge()

        override fun mock(): CloseTransferDeliveryChallenge {
            return CloseTransferDeliveryChallenge(
                AMTreeProof.mock(),
                MerklePath.mock(),
                Address.random(),
                Hash.random()
            )
        }
    }
}
