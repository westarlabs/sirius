pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
import "./safe_math.sol";
import "./byte_util.sol";

library WithdrawalLib {

    using SafeMath for uint;

    struct WithdrawalMeta {
        uint total;
        mapping(address => uint) withdrawals;
    }
}

library DepositLib {

    using SafeMath for uint;

    struct DepositMeta {
        uint total;
        mapping(address => uint) deposits;
    }

    function add(DepositMeta storage self, address addr, uint amount) internal {
        self.total = self.total.add(amount);
        self.deposits[addr] = self.deposits[addr].add(amount);
    }
}

library BalanceLib {
    struct Balance {
        uint eon;
        DepositLib.DepositMeta depositMeta;
        WithdrawalLib.WithdrawalMeta withdrawalMeta;
        HubRootLib.HubRoot root;
        bool hasRoot;
    }
}

library ChainHashLib {
    struct ChainHash {
        bytes32 hash;
    }

    function unmarshal(bytes memory data) internal pure returns (ChainHash memory chainHash) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) {
                bytes memory tmp = RLPLib.toData(r);
                chainHash.hash = ByteUtilLib.bytesToBytes32(tmp);
            }

            idx++;
        }
    }

    function marshal(ChainHash memory self) internal pure returns (bytes memory) {
        return RLPEncoder.encodeList(RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(self.hash)));
    }
}

library NodeInfoLib {
    struct NodeInfo {
        bytes32 left;
        uint amount;
        bytes32 right;
    }

    function unmarshal(bytes memory data) internal pure returns (NodeInfo memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.left = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.amount = RLPDecoder.toUint(r);
            else if (idx == 2) node.right = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshal(NodeInfo memory node) internal pure returns (bytes memory) {
        bytes memory left = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.left));
        bytes memory amount = RLPEncoder.encodeUint(node.amount);
        bytes memory right = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.right));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(left, amount), right));
    }

    function marshal2(NodeInfo memory node) internal pure returns (bytes memory) {
        RLPLib.RLPItem[] memory items;
        RLPLib.RLPItem memory left = RLPDecoder.toRLPItem(ByteUtilLib.bytes32ToBytes(node.left));
        RLPLib.RLPItem memory amount = RLPDecoder.toRLPItem(ByteUtilLib.uint2byte(node.amount));
        RLPLib.RLPItem memory right = RLPDecoder.toRLPItem(ByteUtilLib.bytes32ToBytes(node.right));
        items[0] = left;
        items[1] = amount;
        items[2] = right;

        return RLPEncoder.object2byte(items);
    }
}

library HubRootNodeLib {
    struct HubRootNode {
        uint offset;
        uint allotment;
        NodeInfoLib.NodeInfo info;
    }

    function unmarshal(bytes memory data) internal pure returns (HubRootNode memory rootNode) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) rootNode.offset = RLPDecoder.toUint(r);
            else if (idx == 1) rootNode.info = NodeInfoLib.unmarshal(RLPLib.toData(r));
            else if (idx == 2) rootNode.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshal(HubRootNode memory rootNode) internal pure returns (bytes memory) {
        bytes memory offset = RLPEncoder.encodeUint(rootNode.offset);
        bytes memory info = RLPEncoder.encodeBytes(NodeInfoLib.marshal(rootNode.info));
        bytes memory allotment = RLPEncoder.encodeUint(rootNode.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(offset, info), allotment));
    }
}

library HubRootLib {
    struct HubRoot {
        HubRootNodeLib.HubRootNode node;
        uint eon;
    }

    function unmarshal(bytes memory data) internal pure returns (HubRoot memory root) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) root.node = HubRootNodeLib.unmarshal(RLPLib.toData(r));
            else if (idx == 1) root.eon = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshal(HubRoot memory root) internal pure returns (bytes memory) {
        bytes memory node = RLPEncoder.encodeBytes(HubRootNodeLib.marshal(root.node));
        bytes memory eon = RLPEncoder.encodeUint(root.eon);

        return RLPEncoder.encodeList(ByteUtilLib.append(node, eon));
    }
}

library SignatureLib {
    struct Signature {
        bytes sign;
    }

    function unmarshal(bytes memory data) internal pure returns (SignatureLib.Signature memory signature) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);

        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) signature.sign = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshal(SignatureLib.Signature memory signature) internal pure returns (bytes memory) {
        return RLPEncoder.encodeList(RLPEncoder.encodeBytes(signature.sign));
    }
}

library UpdateLib {
    struct Update {
        bytes32 root;
        uint sendAmount;
        uint receiveAmount;
        uint version;
        SignatureLib.Signature sign;
        SignatureLib.Signature hubSign;
        uint eon;
    }

    function unmarshal(bytes memory data) internal pure returns (UpdateLib.Update memory update) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) update.root = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if(idx == 1) update.sendAmount = RLPDecoder.toUint(r);
            else if(idx == 2) update.receiveAmount = RLPDecoder.toUint(r);
            else if(idx == 3) update.version = RLPDecoder.toUint(r);
            else if(idx == 4) update.sign = SignatureLib.unmarshal(RLPLib.toData(r));
            else if(idx == 5) update.hubSign = SignatureLib.unmarshal(RLPLib.toData(r));
            else if(idx == 6) update.eon = RLPDecoder.toUint(r);
            else {}
        }
    }

    function marshal(UpdateLib.Update memory update) internal pure returns (bytes memory) {
        bytes memory root = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(update.root));
        bytes memory sendAmount = RLPEncoder.encodeUint(update.sendAmount);
        bytes memory receiveAmount = RLPEncoder.encodeUint(update.receiveAmount);
        bytes memory version = RLPEncoder.encodeUint(update.version);
        bytes memory sign = RLPEncoder.encodeBytes(SignatureLib.marshal(update.sign));
        bytes memory hubSign = RLPEncoder.encodeBytes(SignatureLib.marshal(update.hubSign));
        bytes memory eon = RLPEncoder.encodeUint(update.eon);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(root, sendAmount), receiveAmount),version),sign),hubSign),eon));
    }
}

library AccountInfoLib {
    struct AccountInfo {
        bytes32 addr;
        uint allotment;
        UpdateLib.Update update;
    }

    function unmarshal(bytes memory data) internal pure returns (AccountInfoLib.AccountInfo memory accountInfo) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) accountInfo.addr = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if(idx == 1) accountInfo.allotment = RLPDecoder.toUint(r);
            else if(idx == 2) accountInfo.update = UpdateLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(AccountInfoLib.AccountInfo memory accountInfo) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(accountInfo.addr));
        bytes memory allotment = RLPEncoder.encodeUint(accountInfo.allotment);
        bytes memory update = UpdateLib.marshal(accountInfo.update);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(addr, allotment), update));
    }
}

library AugmentedMerkleTreeNodeLib {
    struct AugmentedMerkleTreeNode {
        uint offset;
        NodeInfoLib.NodeInfo node;
        AccountInfoLib.AccountInfo account;
        uint allotment;
    }

    function unmarshal(bytes memory data) internal pure returns (AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) node.offset = RLPDecoder.toUint(r);
            else if(idx == 1) node.node = NodeInfoLib.unmarshal(RLPLib.toData(r));
            else if(idx == 2) node.account = AccountInfoLib.unmarshal(RLPLib.toData(r));
            else if(idx == 3) node.allotment = RLPDecoder.toUint(r);
            else {}
        }
    }

    function marshal(AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory augmentedMerkleTreeNode) internal pure returns (bytes memory) {
        bytes memory offset = RLPEncoder.encodeUint(augmentedMerkleTreeNode.offset);
        bytes memory node = NodeInfoLib.marshal(augmentedMerkleTreeNode.node);
        bytes memory account = AccountInfoLib.marshal(augmentedMerkleTreeNode.account);
        bytes memory allotment = RLPEncoder.encodeUint(augmentedMerkleTreeNode.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(offset, node), account), allotment));
    }
}

library MerklePathDirectionLib {
    enum MerklePathDirection {
        DIRECTION_UNKNOWN,
        DIRECTION_LEFT,
        DIRECTION_RIGTH
    }

    function unmarshal(bytes memory data) internal pure returns (MerklePathDirectionLib.MerklePathDirection dir) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 1) {
                    return MerklePathDirection.DIRECTION_LEFT;
                } else if(tmp == 2) {
                    return MerklePathDirection.DIRECTION_RIGTH;
                }
            } else {}

            idx++;
        }

        return MerklePathDirection.DIRECTION_UNKNOWN;
    }

    function marshal(MerklePathDirectionLib.MerklePathDirection dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == MerklePathDirection.DIRECTION_LEFT) {
            tmp = 1;
        } else if(dir == MerklePathDirection.DIRECTION_RIGTH) {
            tmp = 2;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }
}

library AugmentedMerklePathNodeLib {
    struct AugmentedMerklePathNode {
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode node;
        MerklePathDirectionLib.MerklePathDirection dir;
    }

    function unmarshal(bytes memory data) internal pure returns (AugmentedMerklePathNodeLib.AugmentedMerklePathNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) node.node = AugmentedMerkleTreeNodeLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) node.dir = MerklePathDirectionLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(AugmentedMerklePathNodeLib.AugmentedMerklePathNode memory node) internal pure returns (bytes memory) {
        bytes memory pathNode = AugmentedMerkleTreeNodeLib.marshal(node.node);
        bytes memory dir = MerklePathDirectionLib.marshal(node.dir);

        return RLPEncoder.encodeList(ByteUtilLib.append(pathNode, dir));
    }
}

library AugmentedMerklePathLib {
    struct AugmentedMerklePath {
        uint32 eon;
        AugmentedMerklePathNodeLib.AugmentedMerklePathNode[] nodes;
    }
}
