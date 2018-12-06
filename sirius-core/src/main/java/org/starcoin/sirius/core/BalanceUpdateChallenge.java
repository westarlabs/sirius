package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin;
import org.starcoin.util.KeyPairUtil;

import java.security.PublicKey;
import java.util.Objects;
import java.util.logging.Logger;

public class BalanceUpdateChallenge implements ProtobufCodec<Starcoin.ProtoBalanceUpdateChallenge> {

    private BalanceUpdateProof proof;

    private PublicKey publicKey;

    private ChallengeStatus status;

    private Logger logger = Logger.getLogger(BalanceUpdateChallenge.class.getName());

    public BalanceUpdateChallenge() {
    }

    public BalanceUpdateChallenge(Starcoin.ProtoBalanceUpdateChallenge proto) {
        this.unmarshalProto(proto);
    }

    public BalanceUpdateChallenge(Update update, AugmentedMerklePath provePath, PublicKey publicKey) {
        BalanceUpdateProof proof = new BalanceUpdateProof(update, provePath);
        this.proof = proof;
        this.publicKey = publicKey;
    }

    public BalanceUpdateChallenge(BalanceUpdateProof proof, PublicKey publicKey) {
        this.proof = proof;
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public BalanceUpdateProof getProof() {
        return proof;
    }

    public void setProof(BalanceUpdateProof proof) {
        this.proof = proof;
    }

    public boolean isClosed() {
        synchronized (this) {
            return (this.status != null && this.status == ChallengeStatus.CLOSE);
        }
    }

    public boolean closeChallenge() {
        synchronized (this) {
            if (this.status != null && this.status == ChallengeStatus.OPEN) {
                this.status = ChallengeStatus.CLOSE;
                logger.info("closeChallenge succ");
                return true;
            }

            logger.warning(
                    "closeChallenge err status : " + ((this.status == null) ? "null" : this.status));
            return false;
        }
    }

    public boolean openChallenge() {
        synchronized (this) {
            if (this.status == null) {
                this.status = ChallengeStatus.OPEN;
                logger.info("openChallenge succ");
                return true;
            }

            logger.warning(
                    "openChallenge err status : " + ((this.status == null) ? "null" : this.status));
            return false;
        }
    }

    @Override
    public Starcoin.ProtoBalanceUpdateChallenge marshalProto() {
        Starcoin.ProtoBalanceUpdateChallenge.Builder builder =
                Starcoin.ProtoBalanceUpdateChallenge.newBuilder();
        if (this.proof != null) builder.setProof(this.proof.marshalProto());
        if (this.publicKey != null) {
            builder.setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey)));
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoBalanceUpdateChallenge proto) {
        if (proto.hasProof()) {
            BalanceUpdateProof proof = new BalanceUpdateProof();
            proof.unmarshalProto(proto.getProof());
            this.proof = proof;
        }

        if (!proto.getPublicKey().isEmpty()) {
            this.publicKey = KeyPairUtil.recoverPublicKey(proto.getPublicKey().toByteArray());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BalanceUpdateChallenge)) {
            return false;
        }
        BalanceUpdateChallenge challenge = (BalanceUpdateChallenge) o;
        return Objects.equals(this.proof, challenge.getProof())
                && Objects.equals(this.publicKey, challenge.getPublicKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.proof, this.publicKey);
    }

    public String toString() {
        return this.toJson();
    }

    public static BalanceUpdateChallenge generateBalanceUpdateChallenge(
            Starcoin.ProtoBalanceUpdateChallenge proto) {
        BalanceUpdateChallenge challenge = new BalanceUpdateChallenge();
        challenge.unmarshalProto(proto);
        return challenge;
    }
}
