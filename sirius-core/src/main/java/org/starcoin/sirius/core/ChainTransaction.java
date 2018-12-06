package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor.Marshaller;
import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.*;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoChainTransaction;
import org.starcoin.proto.Starcoin.ProtoChainTransaction.Builder;
import org.starcoin.util.KeyPairUtil;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by dqm on 2018/9/29.
 */
public class ChainTransaction
        implements ProtobufCodec<Starcoin.ProtoChainTransaction>, Hashable, Mockable {
    private BlockAddress from;
    private BlockAddress to;
    private long timestamp;
    private long amount;

    // contract action and arguments
    private String action;
    private byte[] arguments;
    private Receipt receipt;

    private PublicKey publicKey;

    private Signature sign;

    private transient Hash hash;

    public ChainTransaction() {
    }

    public ChainTransaction(BlockAddress from, BlockAddress to, long timestamp, long amount) {
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
        this.amount = amount;
    }

    public ChainTransaction(BlockAddress from, BlockAddress to, long amount) {
        this.from = from;
        this.to = to;
        this.timestamp = System.currentTimeMillis();
        this.amount = amount;
    }

    public ChainTransaction(
            BlockAddress from,
            BlockAddress to,
            long timestamp,
            long amount,
            String action,
            GeneratedMessageV3 arguments) {
        this(from, to, timestamp, amount, action, arguments.toByteString().toByteArray());
    }

    public ChainTransaction(
            BlockAddress from, BlockAddress to, String action, GeneratedMessageV3 arguments) {
        this.from = from;
        this.to = to;
        this.timestamp = System.currentTimeMillis();
        this.amount = 0;
        this.action = action;
        this.arguments = arguments.toByteArray();
    }

    public ChainTransaction(
            BlockAddress from,
            BlockAddress to,
            long timestamp,
            long amount,
            String action,
            byte[] arguments) {
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
        this.amount = amount;
        this.action = action;
        this.arguments = arguments;
    }

    public ChainTransaction(ProtoChainTransaction protoChainTransaction) {
        this.unmarshalProto(protoChainTransaction);
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getAmount() {
        return this.amount;
    }

    public BlockAddress getFrom() {
        return this.from;
    }

    public BlockAddress getTo() {
        return this.to;
    }

    public String getAction() {
        return action;
    }

    public byte[] getArguments() {
        return arguments;
    }

    public <T extends GeneratedMessageV3> T getArguments(Class<T> clazz) {
        if (arguments == null) {
            return null;
        }
        try {
            return (T) clazz.getMethod("parseFrom", byte[].class).invoke(null, this.arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getArguments(Marshaller<T> marshaller) {
        if (arguments == null) {
            return null;
        }
        return marshaller.parse(new ByteArrayInputStream(this.arguments));
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    @Override
    public Starcoin.ProtoChainTransaction marshalProto() {
        Builder builder = this.marshalSignData();
        if (this.sign != null) {
            builder.setSign(this.sign.toProto());
        }
        if (this.receipt != null) {
            builder.setReceipt(this.receipt.toProto());
        }
        if (this.publicKey != null) {
            builder.setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey)));
        }
        return builder.build();
    }

    public Starcoin.ProtoChainTransaction.Builder marshalSignData() {
        ProtoChainTransaction.Builder builder =
                Starcoin.ProtoChainTransaction.newBuilder()
                        .setFrom(this.from.toProto())
                        .setTo(this.to.toProto())
                        .setTimestamp(this.timestamp)
                        .setAmount(this.amount);
        if (this.action != null) {
            builder.setAction(this.action);
        }
        if (this.arguments != null) {
            builder.setArguments(ByteString.copyFrom(this.arguments));
        }
        return builder;
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoChainTransaction proto) {
        this.amount = proto.getAmount();
        this.timestamp = proto.getTimestamp();
        this.from = BlockAddress.valueOf(proto.getFrom());
        this.to = BlockAddress.valueOf(proto.getTo());
        // protobuf string default value is empty string.
        this.action = proto.getAction().isEmpty() ? null : proto.getAction();
        // protobuf bytestring default value is empty bytes.
        this.arguments = proto.getArguments().isEmpty() ? null : proto.getArguments().toByteArray();
        this.receipt = proto.hasReceipt() ? new Receipt(proto.getReceipt()) : null;
        this.sign = proto.hasSign() ? Signature.wrap(proto.getSign()) : null;
        this.publicKey =
                proto.getPublicKey().isEmpty()
                        ? null
                        : KeyPairUtil.recoverPublicKey(proto.getPublicKey().toByteArray());
    }

    public static ChainTransaction generateChainTransaction(Starcoin.ProtoChainTransaction proto) {
        ChainTransaction transaction = new ChainTransaction();
        transaction.unmarshalProto(proto);
        return transaction;
    }

    @Override
    public Hash hash() {
        if (this.hash != null) {
            return this.hash;
        }
        this.hash = Hash.of(this.marshalSignData().build().toByteArray());
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChainTransaction)) {
            return false;
        }
        ChainTransaction that = (ChainTransaction) o;
        return timestamp == that.timestamp
                && amount == that.amount
                && Objects.equals(from, that.from)
                && Objects.equals(to, that.to)
                && Objects.equals(action, that.action)
                && Arrays.equals(arguments, that.arguments)
                && Objects.equals(receipt, that.receipt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, timestamp, amount, action, arguments, receipt);
    }

    @Override
    public void mock(MockContext context) {
        KeyPair keyPair = context.getOrDefault("keyPair", KeyPairUtil.generateKeyPair());
        this.from = BlockAddress.genBlockAddressFromPublicKey(keyPair.getPublic());
        this.to = BlockAddress.random();
        this.amount = RandomUtils.nextLong();
        this.timestamp = System.currentTimeMillis();
        this.publicKey = keyPair.getPublic();
    }

    public boolean isSuccess() {
        return this.receipt == null || this.receipt.isSuccess();
    }

    public void sign(KeyPair keyPair) {
        this.publicKey = keyPair.getPublic();
        this.sign = Signature.of(keyPair.getPrivate(), this.marshalSignData().build().toByteArray());
    }

    public boolean verify() {
        if (this.amount < 0) {
            return false;
        }
        if (this.sign == null) {
            return false;
        }
        if (this.publicKey == null) {
            return false;
        }
        if (this.from.equals(Constants.CONTRACT_ADDRESS)) {
            return true;
        }
        if (!this.from.equals(BlockAddress.genBlockAddressFromPublicKey(this.publicKey))) {
            return false;
        }
        return this.sign.verify(this.publicKey, this.marshalSignData().build().toByteArray());
    }

    public void setFrom(BlockAddress from) {
        this.from = from;
    }

    public void setTo(BlockAddress to) {
        this.to = to;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setArguments(byte[] arguments) {
        this.arguments = arguments;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void setSign(Signature sign) {
        this.sign = sign;
    }

    public void setHash(Hash hash) {
        this.hash = hash;
    }
}
