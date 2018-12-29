package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer

import java.security.PublicKey

@ProtobufSchema(Starcoin.ProtoBalanceUpdateChallenge::class)
@Serializable
data class BalanceUpdateChallenge(
    @SerialId(1)
    val proof: BalanceUpdateProof = BalanceUpdateProof.DUMMY_BALANCE_UPDATE_PROOF,
    @SerialId(2)
    @Serializable(with = PublicKeySerializer::class)
    val publicKey: PublicKey = CryptoService.dummyCryptoKey.keyPair.public
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<BalanceUpdateChallenge, Starcoin.ProtoBalanceUpdateChallenge>(BalanceUpdateChallenge::class) {

        var DUMMY_BALANCE_UPDATE_CHALLENGE = BalanceUpdateChallenge()

        override fun mock(): BalanceUpdateChallenge {
            return BalanceUpdateChallenge(
                BalanceUpdateProof.mock(),
                CryptoService.dummyCryptoKey.keyPair.public
            )
        }
    }
}
