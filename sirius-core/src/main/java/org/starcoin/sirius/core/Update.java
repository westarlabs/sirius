package org.starcoin.sirius.core;

import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.*;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoUpdate;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

public class Update implements ProtobufCodec<ProtoUpdate>, Mockable {

    // transaction root hash.

    private int eon;
    // TODO need two version, one for data structure, another for content update.
    private long version;
    private long sendAmount;
    private long receiveAmount;

    private Hash root;

    private Signature sign;
    private Signature hubSign;

    public Update() {
    }

    //init by eon.
    public Update(int eon) {
        this.eon = eon;
        this.version = 0;
        this.sendAmount = 0;
        this.receiveAmount = 0;
        this.root = null;
    }

    public Update(int eon, long version, long sendAmount, long receiveAmount, Hash root) {
        this.eon = eon;
        this.version = version;
        this.sendAmount = sendAmount;
        this.receiveAmount = receiveAmount;
        this.root = root;
    }

    public Update(int eon, long version, BlockAddress address, List<OffchainTransaction> txs) {
        this.eon = eon;
        this.version = version;
        MerkleTree<OffchainTransaction> tree = new MerkleTree<>(txs);
        this.root = tree.hash();
        this.sendAmount =
                txs.stream()
                        .filter(transaction -> transaction.getFrom().equals(address))
                        .map(OffchainTransaction::getAmount)
                        .reduce(0L, Long::sum);
        this.receiveAmount =
                txs.stream()
                        .filter(transaction -> transaction.getTo().equals(address))
                        .map(OffchainTransaction::getAmount)
                        .reduce(0L, Long::sum);
    }

    public Update(ProtoUpdate update) {
        this.unmarshalProto(update);
    }

    public long getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(long sendAmount) {
        this.sendAmount = sendAmount;
    }

    public long getReceiveAmount() {
        return receiveAmount;
    }

    public void setReceiveAmount(long receiveAmount) {
        this.receiveAmount = receiveAmount;
    }

    public Hash getRoot() {
        return root;
    }

    public void setRoot(Hash root) {
        this.root = root;
    }

    public Signature getSign() {
        return sign;
    }

    public void setSign(Signature sign) {
        this.sign = sign;
    }

    public Signature getHubSign() {
        return hubSign;
    }

    public void setHubSign(Signature hubSign) {
        this.hubSign = hubSign;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public int getEon() {
        return eon;
    }

    public void setEon(int eon) {
        this.eon = eon;
    }

    public ProtoUpdate.Builder marshalSginData() {
        ProtoUpdate.Builder builder =
                ProtoUpdate.newBuilder()
                        .setEon(this.eon)
                        .setVersion(this.version)
                        .setSendAmount(this.sendAmount)
                        .setReceiveAmount(this.receiveAmount);
        if (this.root != null) {
            builder.setRoot(this.root.toProto());
        }
        return builder;
    }

    @Override
    public ProtoUpdate marshalProto() {
        ProtoUpdate.Builder builder = this.marshalSginData();
        if (this.sign != null) {
            builder.setSign(this.sign.toProto());
        }
        if (this.hubSign != null) {
            builder.setHubSign(this.hubSign.toProto());
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(ProtoUpdate proto) {
        this.eon = proto.getEon();
        this.version = proto.getVersion();
        this.sendAmount = proto.getSendAmount();
        this.receiveAmount = proto.getReceiveAmount();
        this.root = proto.hasRoot() ? Hash.wrap(proto.getRoot()) : null;
        this.sign = proto.hasSign() ? Signature.wrap(proto.getSign()) : null;
        this.hubSign = proto.hasHubSign() ? Signature.wrap(proto.getHubSign()) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Update)) {
            return false;
        }
        Update update = (Update) o;
        return eon == update.eon
                && version == update.version
                && sendAmount == update.sendAmount
                && receiveAmount == update.receiveAmount
                && Objects.equals(root, update.root);
    }

    public static Update generateUpdate(Starcoin.ProtoUpdate protoUpdate) {
        if (protoUpdate == null) {
            return null;
        }
        Update update = new Update();
        update.unmarshalProto(protoUpdate);
        return update;
    }

    public boolean verifySig(PublicKey publicKey) {
        if (publicKey == null) {
            return false;
        }
        if (this.sign == null) {
            return false;
        }
        return this.sign.verify(publicKey, this.marshalSginData().build().toByteArray());
    }

    public boolean verifyHubSig(PublicKey publicKey) {
        if (publicKey == null) {
            return false;
        }
        if (this.hubSign == null) {
            return false;
        }
        return this.hubSign.verify(publicKey, this.marshalSginData().build().toByteArray());
    }

    public void sign(PrivateKey privateKey) {
        // TODO optimize resuse bytebuffer
        this.sign = Signature.of(privateKey, this.marshalSginData().build().toByteArray());
    }

    public void signHub(PrivateKey hubPrivateKey) {
        this.hubSign = Signature.of(hubPrivateKey, this.marshalSginData().build().toByteArray());
    }

    public boolean isSigned() {
        return this.sign != null;
    }

    public boolean isSignedByHub() {
        return this.hubSign != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eon, version, sendAmount, receiveAmount, root);
    }

    @Override
    public void mock(MockContext context) {
        this.eon = RandomUtils.nextInt();
        this.version = RandomUtils.nextInt();
        this.sendAmount = RandomUtils.nextInt();
        this.receiveAmount = RandomUtils.nextInt();
        this.root = Hash.random();
    }
}
