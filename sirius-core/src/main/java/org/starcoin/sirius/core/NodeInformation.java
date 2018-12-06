package org.starcoin.sirius.core;

import org.starcoin.core.Hash;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin;

import java.util.Objects;

public class NodeInformation implements ProtobufCodec<Starcoin.ProtoNodeInfo> {

    public static final NodeInformation EMPTY_NODE = new NodeInformation(Hash.ZERO_HASH, 0,
            Hash.ZERO_HASH);

    private Hash left;
    private long offset;
    private Hash right;

    public NodeInformation() {
    }

    public NodeInformation(Hash left, long offset, Hash right) {
        this.left = left;
        this.offset = offset;
        this.right = right;
    }

    public Hash getLeft() {
        return left;
    }

    public long getOffset() {
        return offset;
    }

    public Hash getRight() {
        return right;
    }

    @Override
    public Starcoin.ProtoNodeInfo marshalProto() {
        return Starcoin.ProtoNodeInfo.newBuilder()
                .setLeft(this.left.toProto())
                .setOffset(this.offset)
                .setRight(this.right.toProto())
                .build();
    }

    @Override
    public void unmarshalProto(Starcoin.ProtoNodeInfo proto) {
        this.left = Hash.wrap(proto.getLeft());
        this.offset = proto.getOffset();
        this.right = Hash.wrap(proto.getRight());
    }

    public static NodeInformation generateNodeInformation(Starcoin.ProtoNodeInfo proto) {
        if (proto == null) {
            return null;
        }
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.unmarshalProto(proto);
        return nodeInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeInformation)) {
            return false;
        }
        NodeInformation that = (NodeInformation) o;
        return offset == that.offset &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, offset, right);
    }
}
