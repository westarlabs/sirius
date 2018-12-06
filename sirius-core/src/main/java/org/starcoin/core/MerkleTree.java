package org.starcoin.core;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.MerklePath.Direction;
import org.starcoin.core.MerklePath.MerklePathNode;
import org.starcoin.core.MerkleTree.MerkleTreeData;
import org.starcoin.proto.Starcoin;
import org.starcoin.proto.Starcoin.ProtoMerkleTreeNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MerkleTree<D extends MerkleTreeData> implements Hashable {

    public interface MerkleTreeData<P extends GeneratedMessageV3> extends Hashable, ProtobufCodec<P> {

        String PROTO_OUT_CLASS_NAME = Starcoin.class.getSimpleName();

        Map<Class<? extends GeneratedMessageV3>, Class<? extends MerkleTreeData>> implementsMap =
                new HashMap<>();

        static void registerImplement(
                Class<? extends MerkleTreeData> implementClass,
                Class<? extends GeneratedMessageV3> protobufClass) {
            implementsMap.put(protobufClass, implementClass);
        }

        static <D extends MerkleTreeData> D unpark(Any any) {
            if (any.getValue().isEmpty() || any.getTypeUrl().isEmpty()) {
                return null;
            }
            try {
                //example type.googleapis.com/org.starcoin.proto.ProtoHubTransaction
                String clazzName = any.getTypeUrl().substring(any.getTypeUrl().lastIndexOf('.') + 1);
                String clazzFullname = String
                        .format("%s.%s$%s", Starcoin.getDescriptor().getPackage(), PROTO_OUT_CLASS_NAME,
                                clazzName);
                Class<GeneratedMessageV3> clazz = (Class<GeneratedMessageV3>) Class.forName(clazzFullname);
                GeneratedMessageV3 message = any.unpack(clazz);
                Class<? extends MerkleTreeData> dataClass = implementsMap.get(clazz);
                return (D) dataClass.getConstructor(clazz).newInstance(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class MerkleTreeNode<D extends MerkleTreeData>
            implements ProtobufCodec<ProtoMerkleTreeNode>, Hashable {

        private Hash hash;
        private D data;
        private transient MerkleTreeNode parent;
        private transient MerkleTreeNode left;
        private transient MerkleTreeNode right;

        public MerkleTreeNode(ProtoMerkleTreeNode node) {
            this.unmarshalProto(node);
        }

        public MerkleTreeNode(D data) {
            this.data = data;
        }

        public MerkleTreeNode(MerkleTreeNode left, MerkleTreeNode right) {
            this.left = left;
            this.right = (right == null ? left : right);
            this.left.parent = this;
            this.right.parent = this;
        }

        @Override
        public Hash hash() {
            if (this.hash == null) {
                if (this.data != null) {
                    this.hash = this.data.hash();
                } else {
                    this.hash = Hash.combine(left.hash(), right == null ? null : right.hash());
                }
            }
            return hash;
        }

        public MerkleTreeNode getLeft() {
            return this.left;
        }

        public MerkleTreeNode getRight() {
            return this.right;
        }

        public MerkleTreeNode getParent() {
            return this.parent;
        }

        public D getData() {
            return data;
        }

        public MerkleTreeNode getSibling() {
            if (this.getParent() == null) {
                return null;
            }
            return this == this.getParent().getLeft()
                    ? this.getParent().getRight()
                    : this.getParent().getLeft();
        }

        public Direction getDirection() {
            if (this.getParent() == null) {
                return null;
            }
            return this == this.getParent().getLeft()
                    ? MerklePath.Direction.LEFT
                    : MerklePath.Direction.RIGHT;
        }

        private MerkleTreeNode<D> randomChild() {
            if (this.isLeafNode()) {
                return this;
            }
            if (RandomUtils.nextBoolean()) {
                return this.left;
            } else {
                return this.right;
            }
        }

        private boolean isLeafNode() {
            return this.data != null;
        }

        @Override
        public ProtoMerkleTreeNode marshalProto() {
            ProtoMerkleTreeNode.Builder builder = ProtoMerkleTreeNode.newBuilder();
            if (this.data != null) {
                builder.setData(Any.pack(this.data.marshalProto()));
            } else {
                builder.setHash(this.hash().toProto());
            }
            return builder.build();
        }

        @Override
        public void unmarshalProto(ProtoMerkleTreeNode proto) {
            if (!proto.getData().getValue().isEmpty()) {
                this.data = MerkleTreeData.unpark(proto.getData());
            } else {
                this.hash = Hash.wrap(proto.getHash());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MerkleTreeNode)) {
                return false;
            }
            MerkleTreeNode<?> that = (MerkleTreeNode<?>) o;
            //if node is leaf node, just compare data.
            if (this.data != null) {
                return Objects.equals(data, that.data);
            } else {
                return Objects.equals(this.hash(), that.hash());
            }
        }

        @Override
        public int hashCode() {
            if (this.data != null) {
                return Objects.hash(data);
            } else {
                return Objects.hash(this.hash());
            }
        }
    }

    private MerkleTreeNode root;

    public MerkleTree(List<D> leaves) {
        this.root = this.buildRoot(buildTreeNodes(leaves));
    }

    public MerkleTree(MerklePath<D> path) {
        this.root = this.buildRoot(path);
    }

    public MerkleTreeNode<D> getRoot() {
        return this.root;
    }

    @Override
    public Hash hash() {
        return root.hash();
    }

    public MerkleTreeNode<D> randomLeafNode() {
        MerkleTreeNode node = this.root.randomChild();
        while (!node.isLeafNode()) {
            node = node.randomChild();
        }
        return node;
    }

    public Optional<MerkleTreeNode> findTreeNode(Hash nodeHash) {
        return findTreeNode(this.root, node -> node.hash().equals(nodeHash));
    }

    private Optional<MerkleTreeNode> findTreeNode(
            MerkleTreeNode node, Predicate<MerkleTreeNode> predicate) {
        if (node == null) {
            return Optional.empty();
        }
        if (predicate.test(node)) {
            return Optional.of(node);
        }
        Optional<MerkleTreeNode> found = findTreeNode(node.getLeft(), predicate);
        if (found.isPresent()) {
            return found;
        } else {
            return findTreeNode(node.getRight(), predicate);
        }
    }

    public MerklePath getMembershipProof(Hash nodeHash) {
        Optional<MerkleTreeNode> nodeOptional = this.findTreeNode(nodeHash);
        if (!nodeOptional.isPresent()) {
            return null;
        }
        MerkleTreeNode node = nodeOptional.get();
        MerklePath path = new MerklePath();
        path.append(node, node.getDirection());
        MerkleTreeNode siblingNode = node.getSibling();
        path.append(siblingNode, siblingNode.getDirection());

        MerkleTreeNode parent = node.getParent();
        while (parent.getParent() != null) {
            siblingNode = parent.getSibling();
            path.append(siblingNode, siblingNode.getDirection());
            parent = parent.getParent();
        }
        return path;
    }

    private MerkleTreeNode<D> buildRoot(List<MerkleTreeNode<D>> leaves) {
        List<MerkleTreeNode<D>> mergedLeaves = new ArrayList<>();
        for (int i = 0, n = leaves.size(); i < n; i++) {
            if (i < n - 1) {
                mergedLeaves.add(new MerkleTreeNode<>(leaves.get(i), leaves.get(i + 1)));
                i++;
            } else {
                mergedLeaves.add(new MerkleTreeNode<>(leaves.get(i), null));
            }
        }

        if (mergedLeaves.size() > 1) {
            return buildRoot(mergedLeaves);
        } else {
            return mergedLeaves.get(0);
        }
    }

    private List<MerkleTreeNode<D>> buildTreeNodes(List<D> datas) {
        return datas
                .stream()
                .map(
                        data -> {
                            MerkleTreeNode<D> node = new MerkleTreeNode(data);
                            return node;
                        })
                .collect(Collectors.toList());
    }

    private MerkleTreeNode<D> buildRoot(MerklePath<D> path) {
        MerkleTreeNode<D> node = path.getNodes().get(0).getNode();

        for (int i = 1; i < path.getNodes().size(); i++) {
            MerklePathNode<D> pathNode = path.getNodes().get(i);
            if (pathNode.getDirection() == MerklePath.Direction.LEFT) {
                node = new MerkleTreeNode<>(pathNode.getNode(), node);
            } else {
                node = new MerkleTreeNode<>(node, pathNode.getNode());
            }
        }
        return node;
    }

    public static boolean verifyMembershipProof(MerkleTreeNode root, MerklePath<?> path) {
        MerkleTree tree = new MerkleTree(path);
        return tree.hash().equals(root.hash());
    }

    public static boolean verifyMembershipProof(Hash rootHash, MerklePath<?> path) {
        MerkleTree tree = new MerkleTree(path);
        return tree.hash().equals(rootHash);
    }
}
