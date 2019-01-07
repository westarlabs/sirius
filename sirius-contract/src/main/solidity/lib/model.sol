pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
import "./safe_math.sol";
import "./byte_util.sol";

library GlobleLib {

    using SafeMath for uint;

    ////////////////////////////////////////Withdrawal
    enum WithdrawalStatusType {
        INIT,
        CANCEL,
        CONFIRMED
    }

    function unmarshalWithdrawalStatusType(RLPLib.RLPItem memory rlp) internal pure returns (WithdrawalStatusType stat) {
        uint tmp = RLPDecoder.toUint(rlp);
        if(tmp == 0) {
            return WithdrawalStatusType.INIT;
        } else if(tmp == 1) {
            return WithdrawalStatusType.CANCEL;
        } else if(tmp == 2) {
            return WithdrawalStatusType.CONFIRMED;
        } else {
            revert();
        }
    }

    function marshalWithdrawalStatusType(WithdrawalStatusType stat) internal pure returns (bytes memory) {
        uint tmp;

        if(stat == WithdrawalStatusType.INIT) {
            tmp = 0;
        } else if(stat == WithdrawalStatusType.CANCEL) {
            tmp = 1;
        } else if(stat == WithdrawalStatusType.CONFIRMED) {
            tmp = 2;
        }

        return RLPEncoder.encodeUint(tmp);
    }

    struct Withdrawal {
        bytes info;//bytes for ModelLib.WithdrawalInfo
        WithdrawalStatusType stat;
        bool isVal;
    }

    struct WithdrawalMeta {
        uint total;
        address[] addrs;
        mapping(bytes32 => Withdrawal) withdrawals;
    }

    ////////////////////////////////////////Deposit
    struct Deposit {
        bool hasVal;
        address addr;
        uint amount;
    }

    struct DepositMeta {
        uint total;
        mapping(bytes32 => uint) deposits;
    }

    function deposit(DepositMeta storage self, address addr, uint amount) internal {
        bytes32 key = ByteUtilLib.address2hash(addr);
        self.deposits[key] = SafeMath.add(self.deposits[key], amount);
        self.total = SafeMath.add(self.total, amount);
    }

    ////////////////////////////////////////Transfer challenge

    struct TransferDeliveryChallengeMeta {
        bytes32[] transferChallengeKeys;
        mapping(bytes32 => TransferDeliveryChallengeAndStatus) transferChallenges;
    }

    struct TransferDeliveryChallengeAndStatus {
        bytes challenge;//bytes for ModelLib.TransferDeliveryChallenge
        ModelLib.ChallengeStatus stat;
        bool isVal;
    }

    function change2TransferDeliveryChallenge(TransferDeliveryChallengeAndStatus memory tcs) internal pure returns(ModelLib.TransferDeliveryChallenge memory bs) {
        return ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(tcs.challenge, true));
    }

    ////////////////////////////////////////Balance challenge
    struct BalanceUpdateChallengeAndStatus {
        bytes challenge;//bytes for ModelLib.BalanceUpdateChallenge
        ModelLib.ChallengeStatus status;
        bool isVal;
    }

    function change2BalanceUpdateChallengeStatus(BalanceUpdateChallengeAndStatus memory bas) internal pure returns(ModelLib.BalanceUpdateChallengeStatus memory bs) {
        bs.challenge = ModelLib.unmarshalBalanceUpdateChallenge(RLPDecoder.toRLPItem(bas.challenge, true));
        bs.status = bas.status;
    }

    struct BalanceUpdateChallengeMeta {
        bytes32[] balanceChallengeKeys;
        mapping(bytes32 => BalanceUpdateChallengeAndStatus) balanceChallenges;//Use address hash as the key
    }

    ////////////////////////////////////////Balance

    struct Balance {
        uint eon;
        bool hasRoot;
        ModelLib.HubRoot root;
        DepositMeta depositMeta;
        WithdrawalMeta withdrawalMeta;
        BalanceUpdateChallengeMeta bucMeta;
        TransferDeliveryChallengeMeta tdcMeta;
    }
}

//////////////////////////////////

library ModelLib {

    enum Direction {
        DIRECTION_ROOT,
        DIRECTION_LEFT,
        DIRECTION_RIGTH
    }

    function unmarshalDirection(RLPLib.RLPItem memory rlp) internal pure returns (Direction dir) {
        uint tmp = RLPDecoder.toUint(rlp);
        if(tmp == 0) {
            return Direction.DIRECTION_ROOT;
        } else if(tmp == 1) {
            return Direction.DIRECTION_LEFT;
        } else if(tmp == 2) {
            return Direction.DIRECTION_RIGTH;
        } else {
            revert();
        }
    }

    function marshalDirection(Direction dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == Direction.DIRECTION_ROOT) {
            tmp = 0;
        } else if(dir == Direction.DIRECTION_LEFT) {
            tmp = 1;
        } else if(dir == Direction.DIRECTION_RIGTH) {
            tmp = 2;
        } else {
            revert();
        }

        return RLPEncoder.encodeUint(tmp);
    }

    struct AMTreeInternalNodeInfo {
        bytes32 left;
        uint offset;
        bytes32 right;
    }

    function unmarshalAMTreeInternalNodeInfo(RLPLib.RLPItem memory rlp) internal pure returns (AMTreeInternalNodeInfo memory node) {
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

    function unmarshalAMTreeLeafNodeInfo(RLPLib.RLPItem memory rlp) internal pure returns (AMTreeLeafNodeInfo memory node) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.addressHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.update = unmarshalUpdate(r);
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

    function unmarshalAMTreeProof(RLPLib.RLPItem memory rlp) internal pure returns (AMTreeProof memory proof) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) proof.path = unmarshalAMTreePath(r);
            else if (idx == 1) proof.leaf = unmarshalAMTreePathLeafNode(r);
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

    function unmarshalAMTreeNode(RLPLib.RLPItem memory rlp) internal pure returns (AMTreeNode memory node) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.offset = RLPDecoder.toUint(r);
            else if (idx == 1) node.info = unmarshalAMTreeLeafNodeInfo(r);
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

    function unmarshalAMTree(RLPLib.RLPItem memory rlp) internal pure returns (AMTree memory tree) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) tree.eon = RLPDecoder.toUint(r);
            else if (idx == 1) tree.root = unmarshalAMTreeNode(r);
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

    function unmarshalAMTreePathLeafNode(RLPLib.RLPItem memory rlp) internal pure returns (AMTreePathLeafNode memory leaf) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) leaf.nodeInfo = unmarshalAMTreeLeafNodeInfo(r);
            else if (idx == 1) leaf.direction = unmarshalDirection(r);
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

    function unmarshalAMTreePathInternalNode(RLPLib.RLPItem memory rlp) internal pure returns (AMTreePathInternalNode memory node) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.nodeInfo = unmarshalAMTreeInternalNodeInfo(r);
            else if (idx == 1) node.direction = unmarshalDirection(r);
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

    function verifyMembershipProof4AMTreeProof(AMTreePathInternalNode memory root, AMTreeProof memory proof) internal pure returns(bool) {
        //TODO
        root;
        proof;
        return true;
    }

    function unmarshalAMTreePath(RLPLib.RLPItem memory rlp) internal pure returns (AMTreePath memory path) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) path.eon = RLPDecoder.toUint(r);
            else if(idx == 1) path.leaf = unmarshalAMTreePathLeafNode(r);
            else if(idx == 2) {
                uint len = RLPDecoder.items(r);
                AMTreePathInternalNode[] memory tmp = new AMTreePathInternalNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = unmarshalAMTreePathInternalNode(t);
                    i++;
                }
                path.nodes = tmp;
            } else {}

            idx++;
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

    function unmarshalMerklePathNode(RLPLib.RLPItem memory rlp) internal pure returns (MerklePathNode memory node) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.nodeHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.direction = unmarshalDirection(r);
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

    function unmarshalMerklePath(RLPLib.RLPItem memory rlp) internal pure returns (MerklePath memory path) {
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
                    tmp[i] = unmarshalMerklePathNode(t);
                    i++;
                }
                path.nodes = tmp;
            } else {}

            idx++;
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

    function verifyMembershipProof4Merkle(bytes32 root, MerklePath memory path, bytes32 txHash) internal pure returns(bool flag) {
        root;
        path;
        txHash;
        //TODO
        return true;
    }

    function leaf4MerklePath(MerklePath memory path) internal pure returns (MerklePathNode memory) {
        return path.nodes[0];
    }

//////////////////////////////////

    struct UpdateData {
        uint eon;
        uint version;
        uint sendAmount;
        uint receiveAmount;
        bytes32 root;
    }

    function unmarshalUpdateData(RLPLib.RLPItem memory rlp) internal pure returns (UpdateData memory ud) {
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
        bytes sign;
        bytes hubSign;
    }

    function unmarshalUpdate(RLPLib.RLPItem memory rlp) internal pure returns (Update memory update) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) update.upData = unmarshalUpdateData(r);
            else if (idx == 1) update.sign = RLPLib.toData(r);
            else if (idx == 2) update.hubSign = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalUpdate(Update memory update) internal pure returns (bytes memory) {
        bytes memory upData = marshalUpdateData(update.upData);
        bytes memory sign = RLPEncoder.encodeBytes(update.sign);
        bytes memory hubSign = RLPEncoder.encodeBytes(update.hubSign);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(upData, sign), hubSign));
    }

    function verifySig4Update(bytes memory userPK, Update memory update) internal pure returns(bool flag) {
        //TODO
        userPK;
        update;
        return true;
    }

    function verifyHubSig4Update(bytes memory hubPK, Update memory update) internal pure returns(bool flag) {
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

    function unmarshalWithdrawalInfo(RLPLib.RLPItem memory rlp) internal pure returns (WithdrawalInfo memory init) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) init.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) init.path = unmarshalAMTreePath(r);
            else if(idx == 2) init.amount = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalWithdrawalInfo(WithdrawalInfo memory init) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(init.addr);
        bytes memory path = marshalAMTreePath(init.path);
        bytes memory amount = RLPEncoder.encodeUint(init.amount);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(addr, path), amount));
    }

    function verifyEon4WithdrawalInfo(WithdrawalInfo memory self, uint eon) internal pure {
        require(eon == self.path.eon && eon == self.path.leaf.nodeInfo.update.upData.eon);
    }

    function verifyAddr4WithdrawalInfo(WithdrawalInfo memory self, address addr) internal pure {
        require(addr == self.addr && ByteUtilLib.address2hash(addr) == self.path.leaf.nodeInfo.addressHash);
    }

    struct Participant {
        bytes publicKey;
    }

    function unmarshalParticipant(RLPLib.RLPItem memory rlp) internal pure returns (Participant memory participant) {
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
        address addr;
        Update update;
        AMTreePath path;
    }

    function unmarshalCancelWithdrawal(RLPLib.RLPItem memory rlp) internal pure returns (CancelWithdrawal memory cancel) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cancel.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) cancel.update = unmarshalUpdate(r);
            else if(idx == 2) cancel.path = unmarshalAMTreePath(r);
            else {}

            idx++;
        }
    }

    function marshalCancelWithdrawal(CancelWithdrawal memory cancel) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(cancel.addr);
        bytes memory update = marshalUpdate(cancel.update);
        bytes memory path = marshalAMTreePath(cancel.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(addr, update), path));
    }

    struct BalanceUpdateChallenge {
        BalanceUpdateProof proof;
        bytes publicKey;
    }

    function unmarshalBalanceUpdateChallenge(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateChallenge memory buc) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) buc.proof = unmarshalBalanceUpdateProof(r);
            else if(idx == 1) buc.publicKey = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalBalanceUpdateChallenge(BalanceUpdateChallenge memory buc) internal pure returns (bytes memory) {
        bytes memory proof = marshalBalanceUpdateProof(buc.proof);
        bytes memory publicKey = RLPEncoder.encodeBytes(buc.publicKey);

        return RLPEncoder.encodeList(ByteUtilLib.append(proof, publicKey));
    }

    struct BalanceUpdateProof {
        bool hasUp;
        Update update;
        bool hasPath;
        AMTreeProof proof;
    }

    function unmarshalBalanceUpdateProof(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateProof memory bup) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) bup.hasUp = RLPDecoder.toBool(r);
            else if(idx == 1) bup.update = unmarshalUpdate(r);
            else if(idx == 2) bup.hasPath = RLPDecoder.toBool(r);
            else if(idx == 3) bup.proof = unmarshalAMTreeProof(r);
            else {}

            idx++;
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

    function unmarshalCloseBalanceUpdateChallenge(RLPLib.RLPItem memory rlp) internal pure returns (CloseBalanceUpdateChallenge memory close) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.update = unmarshalUpdate(r);
            else if(idx == 1) close.proof = unmarshalAMTreeProof(r);
            else {}

            idx++;
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

    function unmarshalHubRoot(RLPLib.RLPItem memory rlp) internal pure returns (HubRoot memory hub) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) hub.node = unmarshalAMTreePathInternalNode(r);
            else if(idx == 1) hub.eon = RLPDecoder.toUint(r);
            else {}

            idx++;
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

    function unmarshalOffchainTransactionData(RLPLib.RLPItem memory rlp) internal pure returns (OffchainTransactionData memory offData) {
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

            idx++;
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

    struct OffchainTransaction {
        OffchainTransactionData offData;
        bytes sign;
    }

    function unmarshalOffchainTransaction(RLPLib.RLPItem memory rlp) internal pure returns (OffchainTransaction memory off) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) off.offData = unmarshalOffchainTransactionData(r);
            else if(idx == 1) off.sign = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalOffchainTransaction(OffchainTransaction memory off) internal pure returns (bytes memory) {
        bytes memory offData = marshalOffchainTransactionData(off.offData);
        bytes memory sign = RLPEncoder.encodeBytes(off.sign);

        return RLPEncoder.encodeList(ByteUtilLib.append(offData, sign));
    }

    function hash4OffchainTransaction(OffchainTransaction memory self)  internal pure returns(bytes32) {
        bytes memory bs = marshalOffchainTransaction(self);
        return keccak256(bs);
    }

    function equals4OffchainTransaction(OffchainTransaction memory tran1, OffchainTransaction memory tran2) internal pure returns(bool) {
        return (hash4OffchainTransaction(tran1) == hash4OffchainTransaction(tran2));
    }

    struct TransferDeliveryChallenge {
        Update update;
        OffchainTransaction tran;
        MerklePath path;
    }

    function unmarshalTransferDeliveryChallenge(RLPLib.RLPItem memory rlp) internal pure returns (TransferDeliveryChallenge memory open) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) open.update = unmarshalUpdate(r);
            else if(idx == 1) open.tran = unmarshalOffchainTransaction(r);
            else if(idx == 2) open.path = unmarshalMerklePath(r);
            else {}

            idx++;
        }
    }

    function marshalTransferDeliveryChallenge(TransferDeliveryChallenge memory open) internal pure returns (bytes memory) {
        bytes memory update = marshalUpdate(open.update);
        bytes memory tran = marshalOffchainTransaction(open.tran);
        bytes memory path = marshalMerklePath(open.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(update, tran), path));
    }

    struct CloseTransferDeliveryChallenge {
        AMTreeProof proof;
        Update update;
        MerklePath txPath;
        bytes fromPublicKey;
        bytes32 txHash;
    }

    function unmarshalCloseTransferDeliveryChallenge(RLPLib.RLPItem memory rlp) internal pure returns (CloseTransferDeliveryChallenge memory close) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.proof = unmarshalAMTreeProof(r);
            else if(idx == 1) close.update = unmarshalUpdate(r);
            else if(idx == 2) close.txPath = unmarshalMerklePath(r);
            else if(idx == 3) close.fromPublicKey = RLPLib.toData(r);
            else if(idx == 4) close.txHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalCloseTransferDeliveryChallenge(CloseTransferDeliveryChallenge memory close) internal pure returns (bytes memory) {
        bytes memory proof = marshalAMTreeProof(close.proof);
        bytes memory update = marshalUpdate(close.update);
        bytes memory txPath = marshalMerklePath(close.txPath);
        bytes memory fromPublicKey = RLPEncoder.encodeBytes(close.fromPublicKey);
        bytes memory txHash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(close.txHash));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(proof, update), txPath), fromPublicKey), txHash));
    }

    struct BalanceUpdateChallengeStatus {
        BalanceUpdateChallenge challenge;
        ChallengeStatus status;
    }

    function unmarshalBalanceUpdateChallengeStatus(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateChallengeStatus memory challengeStatus) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) challengeStatus.challenge = unmarshalBalanceUpdateChallenge(r);
            else if(idx == 1) challengeStatus.status = unmarshalChallengeStatus(r);
            else {}

            idx++;
        }
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

    function unmarshalChallengeStatus(RLPLib.RLPItem memory rlp) internal pure returns (ChallengeStatus status) {
        uint tmp = RLPDecoder.toUint(rlp);
        if(tmp == 0) {
            return ChallengeStatus.OPEN;
        } else if(tmp == 1) {
            return ChallengeStatus.CLOSE;
        } else {
            revert();
        }
    }

    function marshalChallengeStatus(ChallengeStatus dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == ChallengeStatus.OPEN) {
            tmp = 0;
        } else if(dir == ChallengeStatus.CLOSE) {
            tmp = 1;
        } else {
            revert();
        }

        return RLPEncoder.encodeUint(tmp);
    }

    struct ContractHubInfo {
        uint startBlockNum;
        string hubAddress;
        uint blocksPerEon;
    }

    function marshalContractHubInfo(ContractHubInfo memory chi) internal pure returns (bytes memory) {
        bytes memory startBlockNum = RLPEncoder.encodeUint(chi.startBlockNum);
        bytes memory hubAddress = RLPEncoder.encodeString(chi.hubAddress);
        bytes memory blocksPerEon = RLPEncoder.encodeUint(chi.blocksPerEon);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(startBlockNum, hubAddress), blocksPerEon));
    }

    enum ContractReturnType {
        CR_WITHDRAWAL,
        CR_BALANCE,
        CR_TRANSFER,
        CR_HUBROOT
    }

    function unmarshalContractReturnType(RLPLib.RLPItem memory rlp) internal pure returns (ContractReturnType crt) {
        uint tmp = RLPDecoder.toUint(rlp);
        if(tmp == 0) {
            return ContractReturnType.CR_WITHDRAWAL;
        } else if(tmp == 1) {
            return ContractReturnType.CR_BALANCE;
        } else if(tmp == 2) {
            return ContractReturnType.CR_TRANSFER;
        } else if(tmp == 3) {
            return ContractReturnType.CR_HUBROOT;
        } else {
            revert();
        }
    }

    function marshalContractReturnType(ContractReturnType crt) internal pure returns (bytes memory) {
        uint tmp;
        if(crt == ContractReturnType.CR_WITHDRAWAL) {
            tmp = 0;
        } else if(crt == ContractReturnType.CR_BALANCE) {
            tmp = 1;
        } else if(crt == ContractReturnType.CR_TRANSFER) {
            tmp = 2;
        } else if(crt == ContractReturnType.CR_HUBROOT) {
            tmp = 3;
        } else {
            revert();
        }

        return RLPEncoder.encodeUint(tmp);
    }

    struct ContractReturn {
       bool hasVal;
       ContractReturnType crt;
       bytes payload;
    }

    function unmarshalContractReturn(RLPLib.RLPItem memory rlp) internal pure returns (ContractReturn memory cr) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cr.hasVal = RLPDecoder.toBool(r);
            else if(idx == 1) cr.crt = unmarshalContractReturnType(r);
            else if(idx == 2) cr.payload = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalContractReturn(ContractReturn memory cr) internal pure returns (bytes memory) {
        bytes memory hasVal = RLPEncoder.encodeBool(cr.hasVal);
        bytes memory crt = marshalContractReturnType(cr.crt);
        bytes memory payload = RLPEncoder.encodeBytes(cr.payload);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(hasVal, crt), payload));
    }
}