package org.starcoin.sirius.core;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.DepositRequest;

import java.util.Objects;

public class Deposit implements ProtobufCodec<DepositRequest> {

    private BlockAddress address;
    private long amount;

    public Deposit() {
    }

    public Deposit(DepositRequest proto) {
        this.unmarshalProto(proto);
    }

    public Deposit(BlockAddress address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public BlockAddress getAddress() {
        return this.address;
    }

    public long getAmount() {
        return this.amount;
    }

    @Override
    public DepositRequest marshalProto() {
        DepositRequest.Builder builder = DepositRequest.newBuilder();
        if (this.address != null) builder.setAddress(this.address.toProto());
        builder.setAmount(this.amount);
        return builder.build();
    }

    @Override
    public void unmarshalProto(DepositRequest proto) {
        this.address = (proto.hasAddress()) ? BlockAddress.valueOf(proto.getAddress()) : null;
        this.amount = proto.getAmount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Deposit)) {
            return false;
        }
        Deposit deposit = (Deposit) o;
        return this.amount == deposit.amount && Objects.equals(this.address, deposit.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.amount);
    }
}
