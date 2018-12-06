package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoParticipant;
import org.starcoin.util.KeyPairUtil;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Objects;

/**
 * Created by dqm on 2018/9/23.
 */
public class Participant implements ProtobufCodec<Starcoin.ProtoParticipant> {

    private BlockAddress address;
    private PublicKey publicKey;

    public Participant() {
    }

    public Participant(ProtoParticipant participant) {
        this.unmarshalProto(participant);
    }

    public Participant(PublicKey publicKey) {
        this.address = BlockAddress.genBlockAddressFromPublicKey(publicKey);
        this.publicKey = publicKey;
    }

    public BlockAddress getAddress() {
        return this.address;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public Starcoin.ProtoParticipant marshalProto() {
        Starcoin.ProtoParticipant.Builder builder = Starcoin.ProtoParticipant.newBuilder();
        if (this.publicKey != null)
            builder.setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey)));
        return builder.build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoParticipant proto) {
        if (!proto.getPublicKey().isEmpty()) {
            this.publicKey = KeyPairUtil.recoverPublicKey(proto.getPublicKey().toByteArray());
            this.address = BlockAddress.genBlockAddressFromPublicKey(this.publicKey);
        }
    }

    public static Participant generateParticipant(Starcoin.ProtoParticipant proto) {
        Participant participant = new Participant();
        participant.unmarshalProto(proto);
        return participant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Participant)) {
            return false;
        }
        Participant that = (Participant) o;
        return Objects.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public static Participant random() {
        KeyPair kp = KeyPairUtil.generateKeyPair();
        return new Participant(kp.getPublic());
    }
}
