package org.starcoin.sirius.core;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.Hash;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoAccountInfo;

import java.util.Objects;

public class AccountInformation implements ProtobufCodec<Starcoin.ProtoAccountInfo> {

    public static final AccountInformation EMPTY_ACCOUNT =
            new AccountInformation(BlockAddress.DEFAULT_ADDRESS, 0, null);

    // just keep hash of address
    private Hash address;
    private long allotment;
    private Update update;

    public AccountInformation() {
    }

    public AccountInformation(BlockAddress address, long allotment, Update update) {
        this.address = Hash.of(address.toBytes());
        this.allotment = allotment;
        this.update = update;
    }

    public AccountInformation(Hash address, long allotment, Update update) {
        this.address = address;
        this.allotment = allotment;
        this.update = update;
    }

    public Hash getAddress() {
        return address;
    }

    public Update getUpdate() {
        return update;
    }

    public void setUpdate(Update update) {
        this.update = update;
    }

    public long getAllotment() {
        return allotment;
    }

    @Override
    public Starcoin.ProtoAccountInfo marshalProto() {
        ProtoAccountInfo.Builder builder =
                Starcoin.ProtoAccountInfo.newBuilder()
                        .setAddress(this.getAddress().toProto())
                        .setAllotment(this.getAllotment());
        if (this.update != null) {
            builder.setUpdate(this.getUpdate().toProto());
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoAccountInfo proto) {
        this.address = Hash.wrap(proto.getAddress());
        this.allotment = proto.getAllotment();
        this.update = proto.hasUpdate() ? new Update(proto.getUpdate()) : null;
    }

    public static AccountInformation generateAccountInformation(Starcoin.ProtoAccountInfo proto) {
        if (proto == null) {
            return null;
        }
        AccountInformation accountInformation = new AccountInformation();
        accountInformation.unmarshalProto(proto);
        return accountInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountInformation)) {
            return false;
        }
        AccountInformation that = (AccountInformation) o;
        return allotment == that.allotment &&
                Objects.equals(address, that.address) &&
                Objects.equals(update, that.update);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, allotment, update);
    }
}
