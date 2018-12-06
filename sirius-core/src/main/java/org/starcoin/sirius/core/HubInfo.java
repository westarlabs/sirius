package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode;
import org.starcoin.proto.Starcoin.ProtoHubInfo;
import org.starcoin.util.KeyPairUtil;

import java.security.PublicKey;
import java.util.Objects;

public class HubInfo implements ProtobufCodec<ProtoHubInfo> {

    private boolean ready;
    private int blocksPerEon;
    private int eon;
    private AugmentedMerkleTreeNode root;
    private PublicKey publicKey;


    public HubInfo() {
    }

    public HubInfo(boolean ready, int blocksPerEon) {
        this.ready = ready;
        this.blocksPerEon = blocksPerEon;
    }

    public HubInfo(boolean ready, int blocksPerEon, int eon,
                   AugmentedMerkleTreeNode root, PublicKey publicKey) {
        this.ready = ready;
        this.blocksPerEon = blocksPerEon;
        this.eon = eon;
        this.root = root;
        this.publicKey = publicKey;
    }

    public HubInfo(ProtoHubInfo proto) {
        this.unmarshalProto(proto);
    }

    @Override
    public ProtoHubInfo marshalProto() {
        ProtoHubInfo.Builder builder =
                ProtoHubInfo.newBuilder().setReady(ready).setBlocksPerEon(blocksPerEon);
        if (this.ready) {
            builder
                    .setEon(eon)
                    .setRoot(root.toProto())
                    .setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey)));
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(ProtoHubInfo proto) {
        this.ready = proto.getReady();
        this.eon = proto.getEon();
        this.root = proto.hasRoot() ? new AugmentedMerkleTreeNode(proto.getRoot()) : null;
        this.publicKey =
                proto.getPublicKey().isEmpty()
                        ? null
                        : KeyPairUtil.recoverPublicKey(proto.getPublicKey().toByteArray());
        this.blocksPerEon = proto.getBlocksPerEon();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HubInfo)) {
            return false;
        }
        HubInfo hubInfo = (HubInfo) o;
        return ready == hubInfo.ready
                && eon == hubInfo.eon
                && blocksPerEon == hubInfo.blocksPerEon
                && Objects.equals(root, hubInfo.root)
                && Objects.equals(publicKey, hubInfo.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ready, eon, root, publicKey, blocksPerEon);
    }

    public String toString() {
        return this.toJson();
    }

    public boolean isReady() {
        return ready;
    }

    public int getBlocksPerEon() {
        return blocksPerEon;
    }

    public int getEon() {
        return eon;
    }

    public AugmentedMerkleTreeNode getRoot() {
        return root;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
