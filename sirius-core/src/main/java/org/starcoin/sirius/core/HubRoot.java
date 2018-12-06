package org.starcoin.sirius.core;

import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.MockContext;
import org.starcoin.core.Mockable;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoHubRoot;

import java.util.Objects;

/**
 * Created by dqm on 2018/9/28.
 */
public class HubRoot implements ProtobufCodec<ProtoHubRoot>, Mockable {

    private NodeInformation node;
    private long offset;
    private long allotment;

    private int eon;

    public HubRoot() {
    }

    public HubRoot(ProtoHubRoot proto) {
        this.unmarshalProto(proto);
    }

    public HubRoot(AugmentedMerkleTreeNode root, int eon) {
        this.eon = eon;
        this.node = root.getInformation();
        this.allotment = root.getAllotment();
        this.offset = root.getOffset();
    }

    public NodeInformation getNode() {
        return node;
    }

    public long getOffset() {
        return offset;
    }

    public long getAllotment() {
        return allotment;
    }

    public int getEon() {
        return this.eon;
    }

    @Override
    public Starcoin.ProtoHubRoot marshalProto() {
        Starcoin.ProtoHubRoot.Builder builder = Starcoin.ProtoHubRoot.newBuilder();
        builder.setEon(this.eon);

        if (this.node != null) {
            AugmentedMerkleTreeNode root =
                    new AugmentedMerkleTreeNode(this.offset, this.node, this.allotment);
            builder.setRoot(root.toProto());
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoHubRoot proto) {
        this.eon = proto.getEon();

        if (proto.hasRoot()) {
            AugmentedMerkleTreeNode root = new AugmentedMerkleTreeNode();
            root.unmarshalProto(proto.getRoot());
            this.offset = root.getOffset();
            this.allotment = root.getAllotment();
            this.node = root.getInformation();
        }
    }

    public static HubRoot generateNodeInformation(Starcoin.ProtoHubRoot proto) {
        HubRoot hubRoot = new HubRoot();
        hubRoot.unmarshalProto(proto);
        return hubRoot;
    }

    public void mock(MockContext context) {
        this.allotment = RandomUtils.nextInt();
        this.offset = RandomUtils.nextInt();
        this.eon = RandomUtils.nextInt();
        this.node = context.getOrDefault("nodeinformation", new NodeInformation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HubRoot)) {
            return false;
        }
        HubRoot hubRoot = (HubRoot) o;
        return this.eon == hubRoot.eon
                && this.offset == hubRoot.offset
                && this.allotment == hubRoot.allotment
                && Objects.equals(this.node, hubRoot.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.node, this.offset, this.allotment, this.eon);
    }

    public AugmentedMerkleTreeNode hubRoot2AugmentedMerkleTreeNode() {
        if (this.node != null) {
            return new AugmentedMerkleTreeNode(this.offset, this.node, this.getAllotment());
        }
        return null;
    }

    @Override
    public String toString() {
        return this.toJson();
    }
}
