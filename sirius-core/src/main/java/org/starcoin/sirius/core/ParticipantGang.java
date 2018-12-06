package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoParticipantGang;
import org.starcoin.util.KeyPairUtil;

import java.security.KeyPair;
import java.security.PrivateKey;

// just for test, mock malicious Participant
public class ParticipantGang implements ProtobufCodec<ProtoParticipantGang> {

    private Participant participant;
    private PrivateKey privateKey;

    public ParticipantGang() {
    }

    public ParticipantGang(Participant participant, PrivateKey privateKey) {
        this.participant = participant;
        this.privateKey = privateKey;
    }

    public Participant getParticipant() {
        return participant;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public ProtoParticipantGang marshalProto() {
        return ProtoParticipantGang.newBuilder()
                .setParticipant(this.participant.toProto())
                .setPrivateKey(ByteString.copyFrom(KeyPairUtil.encodePrivateKey(this.privateKey)))
                .build();
    }

    @Override
    public void unmarshalProto(ProtoParticipantGang proto) {
        this.participant = proto.hasParticipant() ? new Participant(proto.getParticipant()) : null;
        this.privateKey =
                proto.getPrivateKey().isEmpty()
                        ? null
                        : KeyPairUtil.recoverPrivateKey(proto.getPrivateKey().toByteArray());
    }

    public static ParticipantGang random() {
        KeyPair keyPair = KeyPairUtil.generateKeyPair();
        return new ParticipantGang(new Participant(keyPair.getPublic()), keyPair.getPrivate());
    }
}
