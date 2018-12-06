package org.starcoin.sirius.core;

import org.apache.commons.lang3.RandomUtils;
import org.starcoin.core.*;
import org.starcoin.core.MerklePath.Direction;
import org.starcoin.sirius.core.AugmentedMerklePath.AugmentedMerklePathNode;
import org.starcoin.proto.Starcoin.ProtoAugmentedMerkleTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AugmentedMerkleTree {

    public static final class AugmentedMerkleTreeNode
            implements Hashable, ProtobufCodec<ProtoAugmentedMerkleTreeNode> {

        private transient AugmentedMerkleTreeNode parent;
        private transient AugmentedMerkleTreeNode left;
        private transient AugmentedMerkleTreeNode right;
        private transient Hash hash;

        private long offset;
        private NodeInformation information;
        private AccountInformation account;
        private long allotment;

        public AugmentedMerkleTreeNode() {
            this(0, NodeInformation.EMPTY_NODE, 0);
        }

        public AugmentedMerkleTreeNode(long offset, AccountInformation account, long allotment) {
            this.offset = offset;
            this.account = account;
            this.allotment = allotment;
        }

        public AugmentedMerkleTreeNode(long offset, NodeInformation node, long allotment) {
            this.offset = offset;
            this.information = node;
            this.allotment = allotment;
        }

        public AugmentedMerkleTreeNode(AugmentedMerkleTreeNode prev, AccountInformation account) {
            this.offset = prev == null ? 0 : prev.offset + prev.allotment;
            this.account = account;
            this.allotment = account.getAllotment();
        }

        public AugmentedMerkleTreeNode(AugmentedMerkleTreeNode left) {
            this(left, (AugmentedMerkleTreeNode) null);
        }

        public AugmentedMerkleTreeNode(ProtoAugmentedMerkleTreeNode proto) {
            this.unmarshalProto(proto);
        }

        public AugmentedMerkleTreeNode(AugmentedMerkleTreeNode left, AugmentedMerkleTreeNode right) {
            if (right == null) {
                right =
                        new AugmentedMerkleTreeNode(
                                left.offset + left.allotment, AccountInformation.EMPTY_ACCOUNT, 0);
            }
            this.left = left;
            this.right = right;
            this.left.parent = this;
            this.right.parent = this;
            this.offset = left.offset;
            this.allotment = left.allotment + right.allotment;
            this.information = new NodeInformation(left.hash(), right.offset, right.hash());
        }

        @Override
        public Hash hash() {
            if (this.hash != null) {
                return this.hash;
            }
            this.hash = Hash.of(this.toProto().toByteArray());
            return this.hash;
        }

        public boolean isLeafNode() {
            return this.left == null && this.right == null && this.account != null;
        }

        public boolean isNnternalNode() {
            return this.information != null;
        }

        public AccountInformation getAccount() {
            return this.account;
        }

        public NodeInformation getInformation() {
            return information;
        }

        public long getOffset() {
            return offset;
        }

        public long getAllotment() {
            return allotment;
        }

        public AugmentedMerkleTreeNode getParent() {
            return parent;
        }

        public AugmentedMerkleTreeNode getLeft() {
            return left;
        }

        public AugmentedMerkleTreeNode getRight() {
            return right;
        }

        public AugmentedMerkleTreeNode getSibling() {
            if (this.parent == null) {
                return null;
            }
            return this == this.parent.left ? this.parent.right : this.parent.left;
        }

        public Direction getDirection() {
            if (this.parent == null) {
                return null;
            }
            return this == this.parent.left ? MerklePath.Direction.LEFT : MerklePath.Direction.RIGHT;
        }

        @Override
        public ProtoAugmentedMerkleTreeNode marshalProto() {
            ProtoAugmentedMerkleTreeNode.Builder builder =
                    ProtoAugmentedMerkleTreeNode.newBuilder().setOffset(this.offset);
            if (this.account != null) {
                builder.setAccount(this.account.toProto());
            }
            if (this.information != null) {
                builder.setNode(this.information.toProto());
            }
            return builder.setAllotment(this.allotment).build();
        }

        @Override
        public void unmarshalProto(ProtoAugmentedMerkleTreeNode proto) {
            this.offset = proto.getOffset();
            this.account =
                    proto.hasAccount()
                            ? AccountInformation.generateAccountInformation(proto.getAccount())
                            : null;
            this.information =
                    proto.hasNode() ? NodeInformation.generateNodeInformation(proto.getNode()) : null;
            this.allotment = proto.getAllotment();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AugmentedMerkleTreeNode)) {
                return false;
            }
            AugmentedMerkleTreeNode treeNode = (AugmentedMerkleTreeNode) o;
            return offset == treeNode.offset
                    && allotment == treeNode.allotment
                    && Objects.equals(information, treeNode.information)
                    && Objects.equals(account, treeNode.account);
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, information, account, allotment);
        }

        public static AugmentedMerkleTreeNode generateTreeNode(ProtoAugmentedMerkleTreeNode proto) {
            AugmentedMerkleTreeNode treeNode = new AugmentedMerkleTreeNode();
            treeNode.unmarshalProto(proto);
            return treeNode;
        }

        public String toString() {
            return this.toJson();
        }
    }

    private int eon = 1;
    private AugmentedMerkleTreeNode root;

    public AugmentedMerkleTree(int eon) {
        this.eon = eon;
        this.root = new AugmentedMerkleTreeNode(0, AccountInformation.EMPTY_ACCOUNT, 0);
    }

    public AugmentedMerkleTree(int eon, List<AccountInformation> accountInformations) {
        this.eon = eon;
        this.root = buildRoot(buildTreeNodes(accountInformations));
    }

    public int getEon() {
        return eon;
    }

    public AugmentedMerkleTreeNode getRoot() {
        return root;
    }

    public Hash hash() {
        return this.root.hash();
    }

    public long getOffset() {
        return this.root.offset;
    }

    public long getAllotment() {
        return this.root.allotment;
    }

    public NodeInformation getInformation() {
        return this.root.information;
    }

    public Optional<AugmentedMerkleTreeNode> findTreeNode(Hash nodeHash) {
        return findTreeNode(this.root, node -> node.hash().equals(nodeHash));
    }

    public Optional<AccountInformation> findAccountInfomation(BlockAddress blockAddress) {
        Optional<AugmentedMerkleTreeNode> treeNode = this.findLeafNode(blockAddress);
        if (treeNode.isPresent()) {
            return Optional.of(treeNode.get().getAccount());
        } else {
            return Optional.empty();
        }
    }

    public Optional<AugmentedMerkleTreeNode> findLeafNode(BlockAddress blockAddress) {
        return this.findLeafNode(Hash.of(blockAddress.toBytes()));
    }

    private Optional<AugmentedMerkleTreeNode> findLeafNode(Hash blockAddressHash) {
        return findTreeNode(
                this.root,
                node -> node.isLeafNode() && node.getAccount().getAddress().equals(blockAddressHash));
    }

    private Optional<AugmentedMerkleTreeNode> findTreeNode(
            AugmentedMerkleTreeNode node, Predicate<AugmentedMerkleTreeNode> predicate) {
        if (node == null) {
            return Optional.empty();
        }
        if (predicate.test(node)) {
            return Optional.of(node);
        }
        Optional<AugmentedMerkleTreeNode> found = findTreeNode(node.left, predicate);
        if (found.isPresent()) {
            return found;
        } else {
            return findTreeNode(node.right, predicate);
        }
    }

    // just for test.
    public AugmentedMerklePath getRandommProof() {
        return this.getMembershipProof(this.randomLeafNode().getAccount().getAddress());
    }

    public AugmentedMerklePath getMembershipProof(Hash blockAddressHash) {
        Optional<AugmentedMerkleTreeNode> nodeOptional = this.findLeafNode(blockAddressHash);
        if (!nodeOptional.isPresent()) {
            return null;
        }
        AugmentedMerkleTreeNode node = nodeOptional.get();
        AugmentedMerklePath path = new AugmentedMerklePath(this.eon);
        path.append(node, node.getDirection());
        AugmentedMerkleTreeNode siblingNode = node.getSibling();
        path.append(siblingNode, siblingNode.getDirection());

        AugmentedMerkleTreeNode parent = node.getParent();
        while (parent.getParent() != null) {
            siblingNode = parent.getSibling();
            path.append(siblingNode, siblingNode.getDirection());
            parent = parent.getParent();
        }
        return path;
    }

    public AugmentedMerklePath getMembershipProof(BlockAddress blockAddress) {
        return this.getMembershipProof(Hash.of(blockAddress.toBytes()));
    }

    public AugmentedMerkleTreeNode randomLeafNode() {
        AugmentedMerkleTreeNode node = randomChild(this.root);
        while (!node.isLeafNode()) {
            node = randomChild(node);
        }
        return node;
    }

    private AugmentedMerkleTreeNode randomChild(AugmentedMerkleTreeNode node) {
        if (node.isLeafNode()) {
            return node;
        }
        if (RandomUtils.nextBoolean()) {
            return node.left;
        } else {
            return node.right;
        }
    }

    private static List<AugmentedMerkleTreeNode> buildTreeNodes(
            List<AccountInformation> accountInformationList) {
        final AugmentedMerkleTreeNode[] prev = {null};
        return accountInformationList
                .stream()
                .map(
                        accountInformation -> {
                            AugmentedMerkleTreeNode node =
                                    new AugmentedMerkleTreeNode(prev[0], accountInformation);
                            prev[0] = node;
                            return node;
                        })
                .collect(Collectors.toList());
    }

    private static AugmentedMerkleTreeNode buildRoot(List<AugmentedMerkleTreeNode> leaves) {
        if (leaves.isEmpty()) {
            return new AugmentedMerkleTreeNode();
        }
        List<AugmentedMerkleTreeNode> mergedLeaves = new ArrayList<>();
        for (int i = 0, n = leaves.size(); i < n; i++) {
            if (i < n - 1) {
                mergedLeaves.add(new AugmentedMerkleTreeNode(leaves.get(i), leaves.get(i + 1)));
                i++;
            } else {
                mergedLeaves.add(new AugmentedMerkleTreeNode(leaves.get(i)));
            }
        }

        if (mergedLeaves.size() > 1) {
            return buildRoot(mergedLeaves);
        } else {
            return mergedLeaves.get(0);
        }
    }

    public static AugmentedMerkleTreeNode buildRoot(AugmentedMerklePath path) {
        List<AugmentedMerklePathNode> nodes = path.getNodes();

        AugmentedMerkleTreeNode node = nodes.get(0).getNode();

        for (int i = 1; i < nodes.size(); i++) {
            AugmentedMerklePathNode pathNode = nodes.get(i);
            if (pathNode.getDirection() == MerklePath.Direction.LEFT) {
                node = new AugmentedMerkleTreeNode(pathNode.getNode(), node);
            } else {
                node = new AugmentedMerkleTreeNode(node, pathNode.getNode());
            }
        }
        return node;
    }

    public static boolean verifyMembershipProof(
            AugmentedMerkleTreeNode root, AugmentedMerklePath path) {
        AugmentedMerkleTreeNode rootBuild = buildRoot(path);
        return rootBuild.hash().equals(root.hash());
    }

    public static AugmentedMerkleTree random(int count) {
        int eon = 1;
        List<AccountInformation> accountInformationList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int allotment = RandomUtils.nextInt(0, 10000);
            int receive = RandomUtils.nextInt(0, 10000);
            int send = RandomUtils.nextInt(0, allotment + receive);
            AccountInformation a =
                    new AccountInformation(
                            BlockAddress.random(), allotment, new Update(eon, 0, send, receive, Hash.random()));
            accountInformationList.add(a);
        }

        AugmentedMerkleTree tree = new AugmentedMerkleTree(eon, accountInformationList);
        return tree;
    }
}
