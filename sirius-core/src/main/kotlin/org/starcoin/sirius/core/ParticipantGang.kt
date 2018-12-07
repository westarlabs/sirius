package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import org.starcoin.proto.Starcoin.ProtoParticipantGang
import org.starcoin.sirius.util.KeyPairUtil

import java.security.PrivateKey

// just for test, mock malicious Participant
class ParticipantGang : ProtobufCodec<ProtoParticipantGang> {

    var participant: Participant? = null
        private set
    var privateKey: PrivateKey? = null
        private set

    constructor() {}

    constructor(participant: Participant, privateKey: PrivateKey) {
        this.participant = participant
        this.privateKey = privateKey
    }

    override fun marshalProto(): ProtoParticipantGang {
        return ProtoParticipantGang.newBuilder()
            .setParticipant(this.participant!!.toProto())
            .setPrivateKey(ByteString.copyFrom(KeyPairUtil.encodePrivateKey(this.privateKey!!)))
            .build()
    }

    override fun unmarshalProto(proto: ProtoParticipantGang) {
        this.participant = if (proto.hasParticipant()) Participant(proto.participant) else null
        this.privateKey = if (proto.privateKey.isEmpty)
            null
        else
            KeyPairUtil.recoverPrivateKey(proto.privateKey.toByteArray())
    }

    companion object {

        fun random(): ParticipantGang {
            val keyPair = KeyPairUtil.generateKeyPair()
            return ParticipantGang(Participant(keyPair.public), keyPair.private)
        }
    }
}