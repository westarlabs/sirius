package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema

import java.security.PublicKey

@ProtobufSchema(Starcoin.ProtoBalanceUpdateChallenge::class)
@Serializable
data class BalanceUpdateChallenge(
    @SerialId(1)
    var proof: BalanceUpdateProof = BalanceUpdateProof.DUMMY_BALANCE_UPDATE_PROOF,
    @SerialId(2)
    var publicKey: PublicKey = CryptoService.instance.loadPublicKey(ByteArray(32)),
    @SerialId(3)
    var status: WithdrawalStatus = WithdrawalStatus.DUMMY_WITHDRAWAL_STATUS
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<BalanceUpdateChallenge, Starcoin.ProtoBalanceUpdateChallenge>(BalanceUpdateChallenge::class) {

        var DUMMY_BALANCE_UPDATE_CHALLENGE = BalanceUpdateChallenge()

        override fun mock(): BalanceUpdateChallenge {
            return BalanceUpdateChallenge(
                BalanceUpdateProof.mock(),
                CryptoService.instance.loadPublicKey(ByteArray(32)),
                WithdrawalStatus.mock()
            )
        }
    }
}
