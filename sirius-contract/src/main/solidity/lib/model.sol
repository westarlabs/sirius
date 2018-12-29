pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
import "./safe_math.sol";
import "./byte_util.sol";

library GlobleLib {

    using SafeMath for uint;

    enum WithdrawalStatusType {
        INIT,
        CANCEL,
        CONFIRMED
    }

    function unmarshalWithdrawalStatusType(bytes memory data) internal pure returns (WithdrawalStatusType stat) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 1) {
                    return WithdrawalStatusType.CANCEL;
                } else if(tmp == 2) {
                    return WithdrawalStatusType.CONFIRMED;
                }
            } else {}

            idx++;
        }

        return WithdrawalStatusType.INIT;
    }

    function marshalWithdrawalStatusType(WithdrawalStatusType stat) internal pure returns (bytes memory) {
        uint tmp;
        if(stat == WithdrawalStatusType.CANCEL) {
            tmp = 1;
        } else if(stat == WithdrawalStatusType.CONFIRMED) {
            tmp = 2;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }

    struct Withdrawal {
        ModelLib.WithdrawalInfo info;
        WithdrawalStatusType stat;
        bool isVal;
    }

    struct WithdrawalMeta {
        uint total;
        address[] addrs;
        mapping(address => Withdrawal) withdrawals;
    }

    struct RecoveryMeta {
        bool isVal;
    }

    struct Deposit {
        bool hasVal;
        address addr;
        uint amount;
    }

    struct DepositMeta {
        uint total;
        mapping(address => uint) deposits;
    }

    function deposit(DepositMeta storage self, address addr, uint amount) internal {
        self.deposits[addr] = SafeMath.add(self.deposits[addr], amount);
        self.total = SafeMath.add(self.total, amount);
    }

    struct TransferDeliveryChallenge {
        ModelLib.OffchainTransaction tran;
        ModelLib.Update update;
        ModelLib.MerklePath path;
        ModelLib.ChallengeStatus stat;
        bool isVal;
    }

    struct Balance {
        uint eon;
        DepositMeta depositMeta;
        WithdrawalMeta withdrawalMeta;
        ModelLib.HubRoot root;
        mapping(address => ModelLib.BalanceUpdateChallengeStatus) balanceChallenges;
        mapping(string => TransferDeliveryChallenge) transferChallenges;
        bool hasRoot;
    }
}

//////////////////////////////////

library ModelLib {

    enum Direction {
        DIRECTION_ROOT,
        DIRECTION_LEFT,
        DIRECTION_RIGTH
    }

    function unmarshalDirection(bytes memory data) internal pure returns (Direction dir) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 1) {
                    return Direction.DIRECTION_LEFT;
                } else if(tmp == 2) {
                    return Direction.DIRECTION_RIGTH;
                }
            } else {}

            idx++;
        }

        return Direction.DIRECTION_ROOT;
    }

    function marshalDirection(Direction dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == Direction.DIRECTION_LEFT) {
            tmp = 1;
        } else if(dir == Direction.DIRECTION_RIGTH) {
            tmp = 2;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }

    struct AMTreeInternalNodeInfo {
        bytes32 left;
        uint offset;
        bytes32 right;
    }

    function unmarshalAMTreeInternalNodeInfo(bytes memory data) internal pure returns (AMTreeInternalNodeInfo memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.left = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.offset = RLPDecoder.toUint(r);
            else if (idx == 2) node.right = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalAMTreeInternalNodeInfo(AMTreeInternalNodeInfo memory node) internal pure returns (bytes memory) {
        bytes memory left = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.left));
        bytes memory offset = RLPEncoder.encodeUint(node.offset);
        bytes memory right = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.right));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(left, offset), right));
    }

    struct AMTreeLeafNodeInfo {
        bytes32 addressHash;
        Update update;
    }

    function unmarshalAMTreeLeafNodeInfo(bytes memory data) internal pure returns (AMTreeLeafNodeInfo memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.addressHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.update = unmarshalUpdate(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalAMTreeLeafNodeInfo(AMTreeLeafNodeInfo memory node) internal pure returns (bytes memory) {
        bytes memory addressHash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.addressHash));
        bytes memory update = marshalUpdate(node.update);

        return RLPEncoder.encodeList(ByteUtilLib.append(addressHash, update));
    }

    struct AMTreeProof {
        AMTreePath path;
        AMTreePathLeafNode leaf;
    }

    function unmarshalAMTreeProof(bytes memory data) internal pure returns (AMTreeProof memory proof) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) proof.path = unmarshalAMTreePath(RLPLib.toData(r));
            else if (idx == 1) proof.leaf = unmarshalAMTreePathLeafNode(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalAMTreeProof(AMTreeProof memory proof) internal pure returns (bytes memory) {
        bytes memory path = marshalAMTreePath(proof.path);
        bytes memory leaf = marshalAMTreePathLeafNode(proof.leaf);

        return RLPEncoder.encodeList(ByteUtilLib.append(path, leaf));
    }

    struct AMTreeNode {
        uint offset;
        AMTreeLeafNodeInfo info;
        uint allotment;
    }

    function unmarshalAMTreeNode(bytes memory data) internal pure returns (AMTreeNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.offset = RLPDecoder.toUint(r);
            else if (idx == 1) node.info = unmarshalAMTreeLeafNodeInfo(RLPLib.toData(r));
            else if (idx == 2) node.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalAMTreeNode(AMTreeNode memory node) internal pure returns (bytes memory) {
        bytes memory offset = RLPEncoder.encodeUint(node.offset);
        bytes memory info = marshalAMTreeLeafNodeInfo(node.info);
        bytes memory allotment = RLPEncoder.encodeUint(node.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(offset, info), allotment));
    }

    struct AMTree {
        uint eon;
        AMTreeNode root;
    }

    function unmarshalAMTree(bytes memory data) internal pure returns (AMTree memory tree) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) tree.eon = RLPDecoder.toUint(r);
            else if (idx == 1) tree.root = unmarshalAMTreeNode(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalAMTree(AMTree memory tree) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(tree.eon);
        bytes memory root = marshalAMTreeNode(tree.root);

        return RLPEncoder.encodeList(ByteUtilLib.append(eon, root));
    }

    struct AMTreePathLeafNode {
        AMTreeLeafNodeInfo nodeInfo;
        Direction direction;
        uint offset;
        uint allotment;
    }

    function unmarshalAMTreePathLeafNode(bytes memory data) internal pure returns (AMTreePathLeafNode memory leaf) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) leaf.nodeInfo = unmarshalAMTreeLeafNodeInfo(RLPLib.toData(r));
            else if (idx == 1) leaf.direction = unmarshalDirection(RLPLib.toData(r));
            else if (idx == 2) leaf.offset = RLPDecoder.toUint(r);
            else if (idx == 3) leaf.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalAMTreePathLeafNode(AMTreePathLeafNode memory leaf) internal pure returns (bytes memory) {
        bytes memory nodeInfo = marshalAMTreeLeafNodeInfo(leaf.nodeInfo);
        bytes memory direction = marshalDirection(leaf.direction);
        bytes memory offset = RLPEncoder.encodeUint(leaf.offset);
        bytes memory allotment = RLPEncoder.encodeUint(leaf.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(nodeInfo, direction), offset), allotment));
    }

    struct AMTreePathInternalNode {
        AMTreeInternalNodeInfo nodeInfo;
        Direction direction;
        uint offset;
        uint allotment;
    }

    function unmarshalAMTreePathInternalNode(bytes memory data) internal pure returns (AMTreePathInternalNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.nodeInfo = unmarshalAMTreeInternalNodeInfo(RLPLib.toData(r));
            else if (idx == 1) node.direction = unmarshalDirection(RLPLib.toData(r));
            else if (idx == 2) node.offset = RLPDecoder.toUint(r);
            else if (idx == 3) node.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalAMTreePathInternalNode(AMTreePathInternalNode memory node) internal pure returns (bytes memory) {
        bytes memory nodeInfo = marshalAMTreeInternalNodeInfo(node.nodeInfo);
        bytes memory direction = marshalDirection(node.direction);
        bytes memory offset = RLPEncoder.encodeUint(node.offset);
        bytes memory allotment = RLPEncoder.encodeUint(node.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(nodeInfo, direction), offset), allotment));
    }

    struct AMTreePath {
        uint eon;
        AMTreePathLeafNode leaf;
        AMTreePathInternalNode[] nodes;
    }

    function verifyMembershipProof4AMTreePath(AMTreePathInternalNode memory root, AMTreePath memory path) internal pure returns(bool) {
        //TODO
        root;
        path;
        return true;
    }

    function unmarshalAMTreePath(bytes memory data) internal pure returns (AMTreePath memory path) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) path.eon = RLPDecoder.toUint(r);
            else if(idx == 1) path.leaf = unmarshalAMTreePathLeafNode(RLPLib.toData(r));
            else if(idx == 2) {
                uint len = RLPDecoder.items(r);
                AMTreePathInternalNode[] memory tmp = new AMTreePathInternalNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = unmarshalAMTreePathInternalNode(RLPLib.toData(t));
                    i++;
                }
                path.nodes = tmp;
            } else {}
        }
    }

    function marshalAMTreePath(AMTreePath memory path) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(path.eon);
        bytes memory leaf = marshalAMTreePathLeafNode(path.leaf);
        bytes memory data;
        AMTreePathInternalNode[] memory nodes = path.nodes;
        for(uint i=0;i<nodes.length;i++) {
            data = ByteUtilLib.append(data, marshalAMTreePathInternalNode(nodes[i]));
        }

        data = RLPEncoder.encodeList(data);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(eon, leaf), data));
    }

    struct MerklePathNode {
        bytes32 nodeHash;
        Direction direction;
    }

    function unmarshalMerklePathNode(bytes memory data) internal pure returns (MerklePathNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.nodeHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.direction = unmarshalDirection(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalMerklePathNode(MerklePathNode memory node) internal pure returns (bytes memory) {
        bytes memory nodeHash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.nodeHash));
        bytes memory direction = marshalDirection(node.direction);

        return RLPEncoder.encodeList(ByteUtilLib.append(nodeHash, direction));
    }

    struct MerklePath {
        MerklePathNode[] nodes;
    }

    function unmarshalMerklePath(bytes memory data) internal pure returns (MerklePath memory path) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint len = RLPDecoder.items(r);
                MerklePathNode[] memory tmp = new MerklePathNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = unmarshalMerklePathNode(RLPLib.toData(t));
                    i++;
                }
                path.nodes = tmp;
            } else {}
        }
    }

    function marshalMerklePath(MerklePath memory path) internal pure returns (bytes memory) {
        bytes memory data;
        MerklePathNode[] memory nodes = path.nodes;
        for(uint i=0;i<nodes.length;i++) {
            data = ByteUtilLib.append(data, marshalMerklePathNode(nodes[i]));
        }

        data = RLPEncoder.encodeList(data);

        return RLPEncoder.encodeList(data);
    }

    function verifyMembershipProof4Merkle(bytes32 root, MerklePath memory path) internal pure returns(bool flag) {
        root;
        path;
        //TODO
        return true;
    }

    function leaf4MerklePath(MerklePath memory path) internal pure returns (MerklePathNode memory) {
        return path.nodes[0];
    }

//////////////////////////////////

    struct Signature {
        bytes sign;
    }

    function unmarshalSignature(bytes memory data) internal pure returns (Signature memory signature) {
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

    function marshalSignature(Signature memory signature) internal pure returns (bytes memory) {
        return RLPEncoder.encodeList(RLPEncoder.encodeBytes(signature.sign));
    }

    struct UpdateData {
        uint eon;
        uint version;
        uint sendAmount;
        uint receiveAmount;
        bytes32 root;
    }

    function unmarshalUpdateData(bytes memory data) internal pure returns (UpdateData memory ud) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) ud.eon = RLPDecoder.toUint(r);
            else if (idx == 1) ud.version = RLPDecoder.toUint(r);
            else if (idx == 2) ud.sendAmount = RLPDecoder.toUint(r);
            else if (idx == 3) ud.receiveAmount = RLPDecoder.toUint(r);
            else if (idx == 4) ud.root = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalUpdateData(UpdateData memory ud) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(ud.eon);
        bytes memory version = RLPEncoder.encodeUint(ud.version);
        bytes memory sendAmount = RLPEncoder.encodeUint(ud.sendAmount);
        bytes memory receiveAmount = RLPEncoder.encodeUint(ud.receiveAmount);
        bytes memory root = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(ud.root));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(eon, version), sendAmount), receiveAmount), root));
    }

    struct Update {
        UpdateData upData;
        Signature sign;
        Signature hubSign;
    }

    function unmarshalUpdate(bytes memory data) internal pure returns (Update memory update) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) update.upData = unmarshalUpdateData(RLPLib.toData(r));
            else if (idx == 1) update.sign = unmarshalSignature(RLPLib.toData(r));
            else if (idx == 2) update.hubSign = unmarshalSignature(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalUpdate(Update memory update) internal pure returns (bytes memory) {
        bytes memory upData = marshalUpdateData(update.upData);
        bytes memory sign = marshalSignature(update.sign);
        bytes memory hubSign = marshalSignature(update.hubSign);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(upData, sign), hubSign));
    }

    function verifySig4Update(Participant memory participant, Update memory update) internal pure returns(bool flag) {
        //TODO
        participant;
        update;
        return true;
    }

    function verifyHubSig4Update(Update memory update, bytes memory hubPK) internal pure returns(bool flag) {
        hubPK;
        update;
        return true;
    }

///////////////////////////////////////

    struct WithdrawalInfo {
        address addr;
        AMTreePath path;
        uint amount;
    }

    function unmarshalWithdrawalInfo(bytes memory data) internal pure returns (WithdrawalInfo memory init) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) init.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) init.path = unmarshalAMTreePath(RLPLib.toData(r));
            else if(idx == 2) init.amount = RLPDecoder.toUint(r);
            else {}
        }
    }

    function marshalWithdrawalInfo(WithdrawalInfo memory init) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(init.addr);
        bytes memory path = marshalAMTreePath(init.path);
        bytes memory amount = RLPEncoder.encodeUint(init.amount);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(addr, path), amount));
    }

    struct Participant {
        bytes publicKey;
    }

    function verifyParticipant(Participant memory participant) internal pure returns(bool flag){
        //TODO
        participant;//publicKey can comput addr
        return true;
    }

    function unmarshalParticipant(bytes memory data) internal pure returns (Participant memory participant) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);

        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) participant.publicKey = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalParticipant(Participant memory participant) internal pure returns (bytes memory) {
        return RLPEncoder.encodeList(RLPEncoder.encodeBytes(participant.publicKey));
    }

    struct CancelWithdrawal {
        Participant participant;
        Update update;
        AMTreePath path;
    }

    function unmarshalCancelWithdrawal(bytes memory data) internal pure returns (CancelWithdrawal memory cancel) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cancel.participant = unmarshalParticipant(RLPLib.toData(r));
            else if(idx == 1) cancel.update = unmarshalUpdate(RLPLib.toData(r));
            else if(idx == 2) cancel.path = unmarshalAMTreePath(RLPLib.toData(r));
            else {}
        }
    }

    function marshalCancelWithdrawal(CancelWithdrawal memory cancel) internal pure returns (bytes memory) {
        bytes memory participant = marshalParticipant(cancel.participant);
        bytes memory update = marshalUpdate(cancel.update);
        bytes memory path = marshalAMTreePath(cancel.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(participant, update), path));
    }

    struct BalanceUpdateChallenge {
        BalanceUpdateProof proof;
        bytes32 publicKey;
    }

    function unmarshalBalanceUpdateChallenge(bytes memory data) internal pure returns (BalanceUpdateChallenge memory buc) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) buc.proof = unmarshalBalanceUpdateProof(RLPLib.toData(r));
            else if(idx == 1) buc.publicKey = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}
        }
    }

    function marshalBalanceUpdateChallenge(BalanceUpdateChallenge memory buc) internal pure returns (bytes memory) {
        bytes memory proof = marshalBalanceUpdateProof(buc.proof);
        bytes memory publicKey = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(buc.publicKey));

        return RLPEncoder.encodeList(ByteUtilLib.append(proof, publicKey));
    }

    struct BalanceUpdateProof {
        bool hasUp;
        Update update;
        bool hasPath;
        AMTreeProof proof;
    }

    function unmarshalBalanceUpdateProof(bytes memory data) internal pure returns (BalanceUpdateProof memory bup) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) bup.hasUp = RLPDecoder.toBool(r);
            else if(idx == 1) bup.update = unmarshalUpdate(RLPLib.toData(r));
            else if(idx == 2) bup.hasPath = RLPDecoder.toBool(r);
            else if(idx == 3) bup.proof = unmarshalAMTreeProof(RLPLib.toData(r));
            else {}
        }
    }

    function marshalBalanceUpdateProof(BalanceUpdateProof memory bup) internal pure returns (bytes memory) {
        bytes memory hasUp = RLPEncoder.encodeBool(bup.hasUp);
        bytes memory update = marshalUpdate(bup.update);
        bytes memory hasPath = RLPEncoder.encodeBool(bup.hasPath);
        bytes memory proof = marshalAMTreeProof(bup.proof);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(hasUp, update), hasPath), proof));
    }

    struct CloseBalanceUpdateChallenge {
        Update update;
        AMTreeProof proof;
    }

    function unmarshalCloseBalanceUpdateChallenge(bytes memory data) internal pure returns (CloseBalanceUpdateChallenge memory close) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.update = unmarshalUpdate(RLPLib.toData(r));
            else if(idx == 1) close.proof = unmarshalAMTreeProof(RLPLib.toData(r));
            else {}
        }
    }

    function marshalCloseBalanceUpdateChallenge(CloseBalanceUpdateChallenge memory close) internal pure returns (bytes memory) {
        bytes memory update = marshalUpdate(close.update);
        bytes memory proof = marshalAMTreeProof(close.proof);

        return RLPEncoder.encodeList(ByteUtilLib.append(update, proof));
    }

    struct HubRoot {
        AMTreePathInternalNode node;
        uint eon;
    }

    function unmarshalHubRoot(bytes memory data) internal pure returns (HubRoot memory hub) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) hub.node = unmarshalAMTreePathInternalNode(RLPLib.toData(r));
            else if(idx == 1) hub.eon = RLPDecoder.toUint(r);
            else {}
        }
    }

    function marshalHubRoot(HubRoot memory hub) internal pure returns (bytes memory) {
        bytes memory node = marshalAMTreePathInternalNode(hub.node);
        bytes memory eon = RLPEncoder.encodeUint(hub.eon);

        return RLPEncoder.encodeList(ByteUtilLib.append(node, eon));
    }

    struct OffchainTransactionData {
        uint eon;
        address fr;
        address to;
        uint amount;
        uint timestamp;
    }

    function unmarshalOffchainTransactionData(bytes memory data) internal pure returns (OffchainTransactionData memory offData) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) offData.eon = RLPDecoder.toUint(r);
            else if(idx == 1) offData.fr = RLPDecoder.toAddress(r);
            else if(idx == 2) offData.to = RLPDecoder.toAddress(r);
            else if(idx == 3) offData.amount = RLPDecoder.toUint(r);
            else if(idx == 4) offData.timestamp = RLPDecoder.toUint(r);
            else {}
        }
    }

    function marshalOffchainTransactionData(OffchainTransactionData memory offData) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(offData.eon);
        bytes memory fr = RLPEncoder.encodeAddress(offData.fr);
        bytes memory to = RLPEncoder.encodeAddress(offData.to);
        bytes memory amount = RLPEncoder.encodeUint(offData.amount);
        bytes memory timestamp = RLPEncoder.encodeUint(offData.timestamp);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(eon, fr), to), amount), timestamp));
    }

    function hash4OffchainTransactionData(OffchainTransactionData memory offData) internal pure returns (bytes32) {
        //TODO
        offData;
        return keccak256(marshalOffchainTransactionData(offData));
    }

    struct OffchainTransaction {
        OffchainTransactionData offData;
        Signature sign;
    }

    function unmarshalOffchainTransaction(bytes memory data) internal pure returns (OffchainTransaction memory off) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) off.offData = unmarshalOffchainTransactionData(RLPLib.toData(r));
            else if(idx == 1) off.sign = unmarshalSignature(RLPLib.toData(r));
            else {}
        }
    }

    function marshalOffchainTransaction(OffchainTransaction memory off) internal pure returns (bytes memory) {
        bytes memory offData = marshalOffchainTransactionData(off.offData);
        bytes memory sign = marshalSignature(off.sign);

        return RLPEncoder.encodeList(ByteUtilLib.append(offData, sign));
    }

    function hash4OffchainTransaction(OffchainTransaction memory self)  internal pure returns(bytes32) {
        bytes memory bs = marshalOffchainTransaction(self);
        return keccak256(bs);
    }

    function equals4OffchainTransaction(OffchainTransaction memory tran1, OffchainTransaction memory tran2) internal pure returns(bool) {
        return (hash4OffchainTransaction(tran1) == hash4OffchainTransaction(tran2));
    }

    struct OpenTransferDeliveryChallengeRequest {
        Update update;
        OffchainTransaction tran;
        MerklePath path;
    }

    function unmarshalOpenTransferDeliveryChallengeRequest(bytes memory data) internal pure returns (OpenTransferDeliveryChallengeRequest memory open) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) open.update = unmarshalUpdate(RLPLib.toData(r));
            else if(idx == 1) open.tran = unmarshalOffchainTransaction(RLPLib.toData(r));
            else if(idx == 2) open.path = unmarshalMerklePath(RLPLib.toData(r));
            else {}
        }
    }

    function marshalOpenTransferDeliveryChallengeRequest(OpenTransferDeliveryChallengeRequest memory open) internal pure returns (bytes memory) {
        bytes memory update = marshalUpdate(open.update);
        bytes memory tran = marshalOffchainTransaction(open.tran);
        bytes memory path = marshalMerklePath(open.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(update, tran), path));
    }

    struct CloseTransferDeliveryChallenge {
        AMTreeProof proof;
        Update update;
        MerklePath txPath;
        bytes32 fromPublicKey;
    }

    function unmarshalCloseTransferDeliveryChallenge(bytes memory data) internal pure returns (CloseTransferDeliveryChallenge memory close) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.proof = unmarshalAMTreeProof(RLPLib.toData(r));
            else if(idx == 1) close.update = unmarshalUpdate(RLPLib.toData(r));
            else if(idx == 2) close.txPath = unmarshalMerklePath(RLPLib.toData(r));
            else if(idx == 3) close.fromPublicKey = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}
        }
    }

    function marshalCloseTransferDeliveryChallenge(CloseTransferDeliveryChallenge memory close) internal pure returns (bytes memory) {
        bytes memory proof = marshalAMTreeProof(close.proof);
        bytes memory update = marshalUpdate(close.update);
        bytes memory txPath = marshalMerklePath(close.txPath);
        bytes memory fromPublicKey = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(close.fromPublicKey));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(proof, update), txPath), fromPublicKey));
    }

    struct BalanceUpdateChallengeStatus {
        BalanceUpdateChallenge challenge;
        ChallengeStatus status;
        bool isVal;
    }

    function unmarshalBalanceUpdateChallengeStatus(bytes memory data) internal pure returns (BalanceUpdateChallengeStatus memory challengeStatus) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) challengeStatus.challenge = unmarshalBalanceUpdateChallenge(RLPLib.toData(r));
            else if(idx == 1) challengeStatus.status = unmarshalChallengeStatus(RLPLib.toData(r));
            else {}
        }

        challengeStatus.isVal = true;
    }

    function marshalBalanceUpdateChallengeStatus(BalanceUpdateChallengeStatus memory challengeStatus) internal pure returns (bytes memory) {
        bytes memory challenge = marshalBalanceUpdateChallenge(challengeStatus.challenge);
        bytes memory status = marshalChallengeStatus(challengeStatus.status);

        return RLPEncoder.encodeList(ByteUtilLib.append(challenge, status));
    }

    enum ChallengeStatus {
        OPEN,
        CLOSE
    }

    function unmarshalChallengeStatus(bytes memory data) internal pure returns (ChallengeStatus status) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 0) {
                    status = ChallengeStatus.OPEN;
                } else if(tmp == 1) {
                    status = ChallengeStatus.CLOSE;
                }
            } else {}

            idx++;
        }
    }

    function marshalChallengeStatus(ChallengeStatus dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == ChallengeStatus.OPEN) {
            tmp = 0;
        } else if(dir == ChallengeStatus.CLOSE) {
            tmp = 1;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }
}