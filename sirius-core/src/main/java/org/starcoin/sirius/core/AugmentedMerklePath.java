package org.starcoin.sirius.core;

import org.starcoin.core.Hash;
import org.starcoin.core.Hashable;
import org.starcoin.core.MerklePath;
import org.starcoin.core.MerklePath.Direction;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode;
import org.starcoin.proto.Starcoin.ProtoAugmentedMerklePath;
import org.starcoin.proto.Starcoin.ProtoAugmentedMerklePathNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AugmentedMerklePath implements ProtobufCodec<ProtoAugmentedMerklePath>, Hashable {

    public AugmentedMerklePath() {
    }

    public static class AugmentedMerklePathNode
            implements ProtobufCodec<ProtoAugmentedMerklePathNode> {

        private AugmentedMerkleTreeNode node;
        // TODO ensure type, use a flag?
        private Direction direction;

        public AugmentedMerklePathNode() {
        }

        public AugmentedMerklePathNode(AugmentedMerkleTreeNode node, Direction direction) {
            this.node = node;
            this.direction = direction;
        }

        public AugmentedMerklePathNode(ProtoAugmentedMerklePathNode protoPathNode) {
            this.unmarshalProto(protoPathNode);
        }

        public AugmentedMerkleTreeNode getNode() {
            return node;
        }

        public Direction getDirection() {
            return direction;
        }

        @Override
        public ProtoAugmentedMerklePathNode marshalProto() {
            return ProtoAugmentedMerklePathNode.newBuilder()
                    .setNode(this.node.toProto())
                    .setDirection(this.direction.toProto())
                    .build();
        }

        @Override
        public void unmarshalProto(ProtoAugmentedMerklePathNode proto) {
            this.node = AugmentedMerkleTreeNode.generateTreeNode(proto.getNode());
            this.direction = MerklePath.Direction.valueOf(proto.getDirection().getNumber());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AugmentedMerklePathNode)) {
                return false;
            }
            AugmentedMerklePathNode that = (AugmentedMerklePathNode) o;
            return Objects.equals(node, that.node) &&
                    direction == that.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, direction);
        }
    }

    private int eon;
    private List<AugmentedMerklePathNode> nodes;
    private transient Hash hash;

    public AugmentedMerklePath(int eon) {
        this.eon = eon;
        this.nodes = new ArrayList<>();
    }

    public AugmentedMerklePath(ProtoAugmentedMerklePath protoMerklePath) {
        this.unmarshalProto(protoMerklePath);
    }

    public void append(AugmentedMerkleTreeNode node, Direction direction) {
        this.nodes.add(new AugmentedMerklePathNode(node, direction));
        this.hash = null;
    }

    public int getEon() {
        return eon;
    }

    public List<AugmentedMerklePathNode> getNodes() {
        return nodes;
    }

    @Override
    public ProtoAugmentedMerklePath marshalProto() {
        return ProtoAugmentedMerklePath.newBuilder()
                .setEon(this.eon)
                .addAllNodes(this.nodes.stream().map(ProtobufCodec::toProto).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void unmarshalProto(ProtoAugmentedMerklePath proto) {
        this.eon = proto.getEon();
        this.nodes =
                proto
                        .getNodesList()
                        .stream()
                        .map(AugmentedMerklePathNode::new)
                        .collect(Collectors.toList());
    }

    @Override
    public Hash hash() {
        if (this.hash != null) {
            return this.hash;
        }
        this.hash = Hash.of(this.marshalProto().toByteArray());
        return this.hash;
    }

    public AugmentedMerkleTreeNode getLeaf() {
        return this.getNodes().get(0).getNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AugmentedMerklePath)) {
            return false;
        }
        AugmentedMerklePath that = (AugmentedMerklePath) o;
        return eon == that.eon &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eon, nodes);
    }

    public String toString() {
        return this.toJson();
    }
}
