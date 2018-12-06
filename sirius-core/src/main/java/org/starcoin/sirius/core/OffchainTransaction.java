package org.starcoin.sirius.core;

import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.*;
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction;
import org.starcoin.proto.Starcoin.ProtoOffchainTransaction.Builder;
import org.starcoin.util.KeyPairUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public class OffchainTransaction
        implements Hashable,
        ProtobufCodec<ProtoOffchainTransaction>,
        MerkleTree.MerkleTreeData<ProtoOffchainTransaction>,
        Mockable {

    static {
        MerkleTree.MerkleTreeData.registerImplement(
                OffchainTransaction.class, ProtoOffchainTransaction.class);
    }

    private int eon;
    private BlockAddress from;
    private BlockAddress to;
    private long timestamp;
    private long amount;

    private Signature sign;

    public OffchainTransaction() {
        this.timestamp = System.currentTimeMillis();
    }

    public OffchainTransaction(int eon, BlockAddress from, BlockAddress to, long amount) {
        this.eon = eon;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public OffchainTransaction(ProtoOffchainTransaction transaction) {
        this.unmarshalProto(transaction);
    }

    public OffchainTransaction(OffchainTransaction tx) {
        this.eon = tx.getEon();
        this.from = tx.getFrom();
        this.to = tx.getTo();
        this.timestamp = tx.timestamp;
        this.amount = tx.amount;
        this.sign = tx.sign;
    }

    public int getEon() {
        return eon;
    }

    public void setEon(int eon) {
        this.eon = eon;
    }

    public BlockAddress getFrom() {
        return from;
    }

    public void setFrom(BlockAddress from) {
        this.from = from;
    }

    public BlockAddress getTo() {
        return to;
    }

    public void setTo(BlockAddress to) {
        this.to = to;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public Hash hash() {
        return Hash.of(this.marshalSignData().build().toByteArray());
    }

    private ProtoOffchainTransaction.Builder marshalSignData() {
        return ProtoOffchainTransaction.newBuilder()
                .setEon(this.eon)
                .setFrom(this.from.toProto())
                .setTo(this.to.toProto())
                .setAmount(this.amount)
                .setTimestamp(this.timestamp);
    }

    @Override
    public ProtoOffchainTransaction marshalProto() {
        Builder builder = this.marshalSignData();
        if (this.sign != null) {
            builder.setSign(this.sign.toProto());
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(ProtoOffchainTransaction proto) {
        this.eon = proto.getEon();
        this.from = BlockAddress.valueOf(proto.getFrom());
        this.to = BlockAddress.valueOf(proto.getTo());
        this.amount = proto.getAmount();
        this.timestamp = proto.getTimestamp();
        this.sign = proto.hasSign() ? Signature.wrap(proto.getSign()) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffchainTransaction)) {
            return false;
        }
        OffchainTransaction that = (OffchainTransaction) o;
        return eon == that.eon
                && timestamp == that.timestamp
                && amount == that.amount
                && Objects.equals(from, that.from)
                && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eon, from, to, timestamp, amount);
    }

    public static OffchainTransaction genarateHubTransaction(ProtoOffchainTransaction proto) {
        OffchainTransaction transaction = new OffchainTransaction();
        transaction.unmarshalProto(proto);
        return transaction;
    }

    @Override
    public void mock(MockContext context) {
        KeyPair keyPair = context.getOrDefault("keyPair", KeyPairUtil.generateKeyPair());
        this.from = BlockAddress.genBlockAddressFromPublicKey(keyPair.getPublic());
        this.to = BlockAddress.random();
        this.amount = RandomUtils.nextLong();
        this.timestamp = System.currentTimeMillis();
    }

    public void sign(PrivateKey privateKey) {
        this.sign = Signature.of(privateKey, this.marshalSignData().build().toByteArray());
    }

    public boolean verify(PublicKey publicKey) {
        if (this.sign == null) {
            return false;
        }
        if (publicKey == null) {
            return false;
        }
        if (!this.from.equals(BlockAddress.genBlockAddressFromPublicKey(publicKey))) {
            return false;
        }
        return this.sign.verify(publicKey, this.marshalSignData().build().toByteArray());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Signature getSign() {
        return sign;
    }

    public void setSign(Signature sign) {
        this.sign = sign;
    }
}
