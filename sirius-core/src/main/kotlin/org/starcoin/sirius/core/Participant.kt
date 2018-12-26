package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoParticipant
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import java.security.PublicKey

@Serializable
@ProtobufSchema(Starcoin.ProtoParticipant::class)
data class Participant(@Serializable(with = PublicKeySerializer::class) @SerialId(1) val publicKey: PublicKey) :
    SiriusObject() {

    @Transient
    val address: Address = Address.getAddress(publicKey)

    companion object : SiriusObjectCompanion<Participant, ProtoParticipant>(Participant::class) {

        var DUMMY_PARTICIPANT = Participant(CryptoService.getDummyCryptoKey().getKeyPair().public)

        override fun mock(): Participant {
            return random()
        }

        fun random(): Participant {
            val kp = CryptoService.generateCryptoKey()
            return Participant(kp.getKeyPair().public)
        }
    }
}
