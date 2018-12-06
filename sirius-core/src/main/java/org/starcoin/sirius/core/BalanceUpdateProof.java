package org.starcoin.sirius.core;

import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin;

import java.util.Objects;

public class BalanceUpdateProof implements ProtobufCodec<Starcoin.ProtoBalanceUpdateProof> {

    private Update update;

    private AugmentedMerklePath provePath;

    public BalanceUpdateProof() {
    }

    public BalanceUpdateProof(Starcoin.ProtoBalanceUpdateProof proto) {
        this.unmarshalProto(proto);
    }

    public BalanceUpdateProof(Update update, AugmentedMerklePath provePath) {
        this.update = update;
        this.provePath = provePath;
    }

    public Update getUpdate() {
        return this.update;
    }

    public void setUpdate(Update update) {
        this.update = update;
    }

    public AugmentedMerklePath getProvePath() {
        return this.provePath;
    }

    public void setProvePath(AugmentedMerklePath provePath) {
        this.provePath = provePath;
    }

    @Override
    public Starcoin.ProtoBalanceUpdateProof marshalProto() {
        Starcoin.ProtoBalanceUpdateProof.Builder builder =
                Starcoin.ProtoBalanceUpdateProof.newBuilder();
        if (this.update != null) builder.setUpdate(this.update.marshalProto());
        if (this.provePath != null) builder.setPath(this.provePath.marshalProto());

        return builder.build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoBalanceUpdateProof proto) {
        if (proto.hasUpdate()) {
            Update update = new Update();
            update.unmarshalProto(proto.getUpdate());
            this.update = update;
        }

        if (proto.hasPath()) {
            AugmentedMerklePath merklePath = new AugmentedMerklePath(proto.getPath());

            this.provePath = merklePath;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BalanceUpdateProof)) {
            return false;
        }
        BalanceUpdateProof proof = (BalanceUpdateProof) o;
        return Objects.equals(this.update, proof.update)
                && Objects.equals(this.provePath, proof.provePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.update, this.provePath);
    }

    public String toString() {
        return this.toJson();
    }

    public static BalanceUpdateProof generateBalanceUpdateProof(
            Starcoin.ProtoBalanceUpdateProof proto) {
        BalanceUpdateProof proof = new BalanceUpdateProof();
        proof.unmarshalProto(proto);
        return proof;
    }
}
