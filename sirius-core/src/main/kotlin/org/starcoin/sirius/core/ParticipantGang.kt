package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.PrivateKeySerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import java.security.PrivateKey

// just for test, mock malicious Participant
@ProtobufSchema(Starcoin.ProtoParticipantGang::class)
@Serializable
data class ParticipantGang(
    @SerialId(1)
    val participant: Participant,
    @SerialId(2)
    @Serializable(with = PrivateKeySerializer::class)
    val privateKey: PrivateKey
) : SiriusObject() {


    companion object : SiriusObjectCompanion<ParticipantGang, Starcoin.ProtoParticipantGang>(ParticipantGang::class) {

        override fun mock(): ParticipantGang {
            return random()
        }

        fun random(): ParticipantGang {
            val key = CryptoService.generateCryptoKey()
            return ParticipantGang(Participant(key.keyPair.public), key.keyPair.private)
        }
    }
}
