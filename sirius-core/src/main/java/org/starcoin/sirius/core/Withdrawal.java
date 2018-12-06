package org.starcoin.sirius.core;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.InitiateWithdrawalRequest;

import java.util.Objects;

public class Withdrawal implements ProtobufCodec<InitiateWithdrawalRequest> {

    private BlockAddress address;
    private AugmentedMerklePath path;
    private long amount;

    public Withdrawal(InitiateWithdrawalRequest proto) {
        this.unmarshalProto(proto);
    }

    public Withdrawal(BlockAddress address, AugmentedMerklePath path, long amount) {
        this.address = address;
        this.path = path;
        this.amount = amount;
    }

    public AugmentedMerklePath getPath() {
        return path;
    }

    public long getAmount() {
        return amount;
    }

    public BlockAddress getAddress() {
        return address;
    }

    @Override
    public InitiateWithdrawalRequest marshalProto() {
        return InitiateWithdrawalRequest.newBuilder()
                .setPath(path.toProto())
                .setAddress(this.address.toProto())
                .setAmount(amount)
                .build();
    }

    @Override
    public void unmarshalProto(InitiateWithdrawalRequest proto) {
        this.address = BlockAddress.valueOf(proto.getAddress());
        this.path = new AugmentedMerklePath(proto.getPath());
        this.amount = proto.getAmount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Withdrawal)) {
            return false;
        }
        Withdrawal that = (Withdrawal) o;
        return amount == that.amount
                && Objects.equals(address, that.address)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, path, amount);
    }

    public String toString() {
        return this.toJson();
    }
}
