package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer

import java.security.PublicKey

@ProtobufSchema(Starcoin.BalanceUpdateChallenge::class)
@Serializable
data class BalanceUpdateChallenge(
    @SerialId(1)
    val proof: BalanceUpdateProof = BalanceUpdateProof.DUMMY_BALANCE_UPDATE_PROOF,
    @SerialId(2)
    @Serializable(with = PublicKeySerializer::class)
    val publicKey: PublicKey = CryptoService.dummyCryptoKey.keyPair.public
) : SiriusObject() {

    companion object :
        SiriusObjectCompanion<BalanceUpdateChallenge, Starcoin.BalanceUpdateChallenge>(BalanceUpdateChallenge::class) {

        var DUMMY_BALANCE_UPDATE_CHALLENGE = BalanceUpdateChallenge()

        override fun mock(): BalanceUpdateChallenge {
            return BalanceUpdateChallenge(
                BalanceUpdateProof.mock(),
                CryptoService.dummyCryptoKey.keyPair.public
            )
        }
    }
}

@ProtobufSchema(Starcoin.BalanceUpdateChallengeStatus::class)
@Serializable
data class BalanceUpdateChallengeStatus(
    @SerialId(1) val challenge: BalanceUpdateChallenge = BalanceUpdateChallenge.DUMMY_BALANCE_UPDATE_CHALLENGE, @SerialId(
        2
    ) val status: ChallengeStatus = ChallengeStatus.OPEN
) :
    SiriusObject() {
    companion object :
        SiriusObjectCompanion<BalanceUpdateChallengeStatus, Starcoin.BalanceUpdateChallengeStatus>(
            BalanceUpdateChallengeStatus::class
        ) {

        var DUMMY_BALANCE_UPDATE_CHALLENGE = BalanceUpdateChallengeStatus()

        override fun mock(): BalanceUpdateChallengeStatus {
            return BalanceUpdateChallengeStatus(
                BalanceUpdateChallenge.mock(),
                ChallengeStatus.OPEN
            )
        }
    }
}
