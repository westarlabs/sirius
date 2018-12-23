package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoParticipant
import org.starcoin.sirius.util.KeyPairUtil
import java.security.PublicKey
import java.util.*

class Participant : ProtobufCodec<Starcoin.ProtoParticipant> {

    var address: Address? = null
        private set
    var publicKey: PublicKey? = null
        private set

    constructor() {}

    constructor(participant: ProtoParticipant) {
        this.unmarshalProto(participant)
    }

    constructor(publicKey: PublicKey) {
        this.address = Address.getAddress(publicKey)
        this.publicKey = publicKey
    }

    override fun marshalProto(): Starcoin.ProtoParticipant {
        val builder = Starcoin.ProtoParticipant.newBuilder()
        if (this.publicKey != null)
            builder.publicKey = ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey!!))
        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoParticipant) {
        if (!proto.publicKey.isEmpty) {
            this.publicKey = KeyPairUtil.recoverPublicKey(proto.publicKey.toByteArray())
            this.address = Address.getAddress(this.publicKey!!)
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Participant) {
            return false
        }
        val that = o as Participant?
        return publicKey == that!!.publicKey
    }

    override fun hashCode(): Int {
        return Objects.hash(address)
    }

    companion object {

        fun generateParticipant(proto: Starcoin.ProtoParticipant): Participant {
            val participant = Participant()
            participant.unmarshalProto(proto)
            return participant
        }

        fun random(): Participant {
            val kp = KeyPairUtil.generateKeyPair()
            return Participant(kp.public)
        }
    }
}
