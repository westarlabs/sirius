package org.starcoin.core;

import org.starcoin.core.MerkleTree.MerkleTreeData;
import org.starcoin.core.MerkleTree.MerkleTreeNode;
import org.starcoin.proto.Starcoin.ProtoMerklePath;
import org.starcoin.proto.Starcoin.ProtoMerklePathDirection;
import org.starcoin.proto.Starcoin.ProtoMerklePathNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MerklePath<D extends MerkleTreeData> implements ProtobufCodec<ProtoMerklePath> {

    private List<MerklePathNode<D>> nodes;

    public MerklePath() {
        this.nodes = new ArrayList<>();
    }

    public MerklePath(ProtoMerklePath proto) {
        this.unmarshalProto(proto);
    }

    public void append(MerkleTreeNode<D> node, Direction direction) {
        this.nodes.add(new MerklePathNode<>(node, direction));
    }

    public List<MerklePathNode<D>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public ProtoMerklePath marshalProto() {
        return ProtoMerklePath.newBuilder()
                .addAllNodes(this.nodes.stream().map(MerklePathNode::toProto).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void unmarshalProto(ProtoMerklePath proto) {
        this.nodes =
                proto
                        .getNodesList()
                        .stream()
                        .map((Function<ProtoMerklePathNode, MerklePathNode<D>>) MerklePathNode::new)
                        .collect(Collectors.toList());
    }

    public MerklePathNode getLeafNode() {
        // TODO
        return nodes.get(0);
    }

    public static MerklePath generateMerklePath(ProtoMerklePath proto) {
        MerklePath path = new MerklePath();
        path.unmarshalProto(proto);
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MerklePath)) {
            return false;
        }
        MerklePath<?> that = (MerklePath<?>) o;
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    public enum Direction implements ProtoEnum<ProtoMerklePathDirection> {
        UNKNOWN(ProtoMerklePathDirection.DIRECTION_UNKNOWN),
        LEFT(ProtoMerklePathDirection.DIRECTION_LEFT),
        RIGHT(ProtoMerklePathDirection.DIRECTION_RIGTH);

        private ProtoMerklePathDirection protoType;

        Direction(ProtoMerklePathDirection protoType) {
            this.protoType = protoType;
        }

        @Override
        public ProtoMerklePathDirection toProto() {
            return protoType;
        }

        @Override
        public int getNumber() {
            return this.protoType.getNumber();
        }

        public static Direction valueOf(int number) {
            for (Direction direction : Direction.values()) {
                if (direction.getNumber() == number) {
                    return direction;
                }
            }
            return Direction.UNKNOWN;
        }
    }

    public static class MerklePathNode<D extends MerkleTreeData>
            implements ProtobufCodec<ProtoMerklePathNode> {

        private MerkleTreeNode<D> node;
        // TODO ensure type, use a flag?
        private Direction direction;

        public MerklePathNode() {
        }

        public MerklePathNode(ProtoMerklePathNode proto) {
            this.unmarshalProto(proto);
        }

        public MerklePathNode(MerkleTreeNode<D> node, Direction direction) {
            this.node = node;
            this.direction = direction;
        }

        public MerkleTreeNode<D> getNode() {
            return node;
        }

        public Direction getDirection() {
            return direction;
        }

        @Override
        public ProtoMerklePathNode marshalProto() {
            return ProtoMerklePathNode.newBuilder()
                    .setNode(node.toProto())
                    .setDirection(this.direction.toProto())
                    .build();
        }

        @Override
        public void unmarshalProto(ProtoMerklePathNode proto) {
            this.node = new MerkleTreeNode<>(proto.getNode());
            this.direction = Direction.valueOf(proto.getDirection().getNumber());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MerklePathNode)) {
                return false;
            }
            MerklePathNode<?> that = (MerklePathNode<?>) o;
            return Objects.equals(node, that.node) && direction == that.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, direction);
        }


        @Override
        public String toString() {
            return this.toJson();
        }

    }
}
