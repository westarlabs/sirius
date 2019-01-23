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

    function marshalWithdrawal(Withdrawal memory w) internal pure returns (bytes memory) {
        bytes memory stat = marshalWithdrawalStatusType(w.stat);
        //no need for isVal

        return RLPEncoder.encodeList(ByteUtilLib.append(w.info, stat));
    }

    struct WithdrawalMeta {
        uint total;
        address payable[] addrs;
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

    function change2TransferDeliveryChallenge(TransferDeliveryChallengeAndStatus memory tdcas) internal pure returns(ModelLib.TransferDeliveryChallenge memory bs) {
        return ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(tdcas.challenge, true));
    }

    function marshalTransferDeliveryChallengeAndStatus(TransferDeliveryChallengeAndStatus memory tdcas) internal pure returns (bytes memory) {
        bytes memory stat = ModelLib.marshalChallengeStatus(tdcas.stat);

        return RLPEncoder.encodeList(ByteUtilLib.append(tdcas.challenge, stat));
    }

    ////////////////////////////////////////Balance challenge
    struct BalanceUpdateChallengeAndStatus {
        bytes challenge;//bytes for ModelLib.BalanceUpdateProof
        ModelLib.ChallengeStatus status;
        bool isVal;
    }

    function change2BalanceUpdateChallengeStatus(BalanceUpdateChallengeAndStatus memory bas) internal pure returns(ModelLib.BalanceUpdateChallengeStatus memory bs) {
        bs.proof = ModelLib.unmarshalBalanceUpdateProof(RLPDecoder.toRLPItem(bas.challenge, true));
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

    struct DataStore {
        mapping (uint => mapping (address => uint)) depositData;
        mapping (uint => mapping (address => Withdrawal)) withdrawalData;
        mapping (uint => mapping (address => BalanceUpdateChallengeAndStatus)) bucData;
        mapping (uint => mapping (address => TransferDeliveryChallengeAndStatus)) tdcData;
    }
}

//////////////////////////////////

library ModelLib {

    enum Direction {
        DIRECTION_ROOT,
        DIRECTION_LEFT,
        DIRECTION_RIGHT
    }

    function unmarshalDirection(RLPLib.RLPItem memory rlp) internal pure returns (Direction dir) {
        uint tmp = RLPDecoder.toUint(rlp);
        if(tmp == 0) {
            return Direction.DIRECTION_ROOT;
        } else if(tmp == 1) {
            return Direction.DIRECTION_LEFT;
        } else if(tmp == 2) {
            return Direction.DIRECTION_RIGHT;
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
        } else if(dir == Direction.DIRECTION_RIGHT) {
            tmp = 2;
        } else {
            revert();
        }

        return RLPEncoder.encodeUint(tmp);
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

    struct CloseBalanceUpdateChallenge {
        address addr;
        AMTreeProof proof;
    }

    function unmarshalCloseBalanceUpdateChallenge(RLPLib.RLPItem memory rlp) internal pure returns (CloseBalanceUpdateChallenge memory close) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) close.proof = unmarshalAMTreeProof(r);
            else {}

            idx++;
        }
    }

    function marshalCloseBalanceUpdateChallenge(CloseBalanceUpdateChallenge memory cancel) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(cancel.addr);
        bytes memory proof = marshalAMTreeProof(cancel.proof);

        return RLPEncoder.encodeList(ByteUtilLib.append(addr, proof));
    }

    struct AMTreeProof {
        AMTreePath path;
        AMTreeLeafNodeInfo leaf;
    }

    function unmarshalAMTreeProof(RLPLib.RLPItem memory rlp) internal pure returns (AMTreeProof memory proof) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) proof.path = unmarshalAMTreePath(r);
            else if (idx == 1) proof.leaf = unmarshalAMTreeLeafNodeInfo(r);
            else {}

            idx++;
        }
    }

    function marshalAMTreeProof(AMTreeProof memory proof) internal pure returns (bytes memory) {
        bytes memory path = marshalAMTreePath(proof.path);
        bytes memory leaf = marshalAMTreeLeafNodeInfo(proof.leaf);

        return RLPEncoder.encodeList(ByteUtilLib.append(path, leaf));
    }

    function verifyProof(uint eon, address userAddr, address hubAddr, AMTreeProof memory proof) internal pure {
        require(eon == proof.path.eon);
        require(ByteUtilLib.address2hash(userAddr) == proof.leaf.addressHash);
        require(verifySign4Update(proof.leaf.update.upData, proof.leaf.update.sign, userAddr));
        require(verifySign4Update(proof.leaf.update.upData, proof.leaf.update.hubSign, hubAddr));
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

    struct AMTreeInternalNodeInfo {
        bytes32 left;
        uint offset;
        bytes32 right;
    }

    function marshalAMTreeInternalNodeInfo(AMTreeInternalNodeInfo memory node) internal pure returns (bytes memory) {
        bytes memory left = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.left));
        bytes memory offset = RLPEncoder.encodeUint(node.offset);
        bytes memory right = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.right));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(left, offset), right));
    }

    struct AMTreePathNode {
        bytes32 nodeHash;
        Direction direction;
        uint offset;
        uint allotment;
    }

    function unmarshalAMTreePathNode(RLPLib.RLPItem memory rlp) internal pure returns (AMTreePathNode memory node) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint len = RLPDecoder.items(rlp);
        require(len == 4, "AMTreePathNode unmarshal err");
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.nodeHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if (idx == 1) node.direction = unmarshalDirection(r);
            else if (idx == 2) node.offset = RLPDecoder.toUint(r);
            else if (idx == 3) node.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalAMTreePathNode(AMTreePathNode memory node) internal pure returns (bytes memory) {
        bytes memory nodeHash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.nodeHash));
        bytes memory direction = marshalDirection(node.direction);
        bytes memory offset = RLPEncoder.encodeUint(node.offset);
        bytes memory allotment = RLPEncoder.encodeUint(node.allotment);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(nodeHash, direction), offset), allotment));
    }

    struct AMTreePath {
        uint eon;
        AMTreePathNode leaf;
        AMTreePathNode[] nodes;
    }

    function verifyMembershipProof4AMTreeProof(AMTreePathNode memory root, AMTreeProof memory proof) internal pure returns(bool) {
        //AMTreeLeafNodeInfo == AMTreePath.leaf
        bytes32 leafHash = keccak256(marshalAMTreeLeafNodeInfo(proof.leaf));
        require(leafHash == proof.path.leaf.nodeHash);

        return verifyMembershipProof4AMTreePath(root, proof.path);
    }

    function verifyMembershipProof4AMTreePath(AMTreePathNode memory root, AMTreePath memory path) internal pure returns(bool) {
        //AMTreePath -> root
        AMTreePathNode memory computeNode = path.leaf;
        for (uint i=0;i<path.nodes.length;i++) {
            AMTreePathNode memory node = path.nodes[i];
            if (node.direction == Direction.DIRECTION_LEFT) {
                computeNode.direction = Direction.DIRECTION_RIGHT;
                computeNode = combineAMTreePathNode(node, computeNode);
                //printAMTreeInternalNodeInfo(computeNode);
            } else if(node.direction == Direction.DIRECTION_RIGHT) {
                computeNode.direction = Direction.DIRECTION_LEFT;
                computeNode = combineAMTreePathNode(computeNode, node);
                //printAMTreeInternalNodeInfo(computeNode);
            } else {}
        }

        computeNode.direction = Direction.DIRECTION_ROOT;

        //hash == hash
        return (keccak256(marshalAMTreePathNode(root)) == keccak256(marshalAMTreePathNode(computeNode)) && root.offset == computeNode.offset && root.allotment == computeNode.allotment);
    }

    function combineAMTreePathNode(AMTreePathNode memory left, AMTreePathNode memory right) private pure returns (AMTreePathNode memory node) {
        AMTreeInternalNodeInfo memory tmp;
        tmp.left = left.nodeHash;
        tmp.offset = right.offset;
        tmp.right = right.nodeHash;

        bytes32 nodeHash = keccak256(marshalAMTreeInternalNodeInfo(tmp));

        node.nodeHash = nodeHash;
        node.offset = left.offset;
        node.allotment = SafeMath.add(left.allotment, right.allotment);
    }

    function unmarshalAMTreePath(RLPLib.RLPItem memory rlp) internal pure returns (AMTreePath memory path) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) path.eon = RLPDecoder.toUint(r);
            else if(idx == 1) path.leaf = unmarshalAMTreePathNode(r);
            else if(idx == 2) {
                uint len = RLPDecoder.items(r);
                AMTreePathNode[] memory tmp = new AMTreePathNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = unmarshalAMTreePathNode(t);
                    i++;
                }
                path.nodes = tmp;
            } else {}

            idx++;
        }
    }

    function marshalAMTreePath(AMTreePath memory path) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(path.eon);
        bytes memory leaf = marshalAMTreePathNode(path.leaf);
        bytes memory data;
        AMTreePathNode[] memory nodes = path.nodes;
        for(uint i=0;i<nodes.length;i++) {
            data = ByteUtilLib.append(data, marshalAMTreePathNode(nodes[i]));
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
        bytes32 hash = txHash;
        for (uint i=0;i<path.nodes.length;i++) {
            MerklePathNode memory node = path.nodes[i];

            if (node.direction == Direction.DIRECTION_LEFT) {
                hash = keccak256(abi.encodePacked(node.nodeHash, hash));
            } else if(node.direction == Direction.DIRECTION_RIGHT) {
                hash = keccak256(abi.encodePacked(hash, node.nodeHash));
            } else {}
        }

        return root == hash;
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

    function updateDataHash(UpdateData memory ud) private pure returns (bytes memory) {
        bytes memory eon = ByteUtilLib.uint2byte(ud.eon);
        bytes memory version = ByteUtilLib.uint2byte(ud.version);
        bytes memory sendAmount = ByteUtilLib.uint2byte(ud.sendAmount);
        bytes memory receiveAmount = ByteUtilLib.uint2byte(ud.receiveAmount);
        bytes memory root = ByteUtilLib.bytes32ToBytes(ud.root);

        return ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(eon, version), sendAmount), receiveAmount), root);
    }

    function verifySign4Update(UpdateData memory data, Signature memory sign, address addr) internal pure returns (bool flag) {
        bytes32 hash = keccak256(marshalUpdateData(data));
        address signer = ecrecover(hash, uint8(sign.v), sign.r, sign.s);
        return signer == addr;
    }

    struct Update {
        UpdateData upData;
        Signature sign;
        Signature hubSign;
    }

    function unmarshalUpdate(RLPLib.RLPItem memory rlp) internal pure returns (Update memory update) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) update.upData = unmarshalUpdateData(r);
            else if (idx == 1) update.sign = unmarshalSignature(r);
            else if (idx == 2) update.hubSign = unmarshalSignature(r);
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

///////////////////////////////////////

    struct WithdrawalInfo {
        AMTreeProof proof;
        uint amount;
    }

    function unmarshalWithdrawalInfo(RLPLib.RLPItem memory rlp) internal pure returns (WithdrawalInfo memory init) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) init.proof = unmarshalAMTreeProof(r);
            else if(idx == 1) init.amount = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalWithdrawalInfo(WithdrawalInfo memory init) internal pure returns (bytes memory) {
        bytes memory proof = marshalAMTreeProof(init.proof);
        bytes memory amount = RLPEncoder.encodeUint(init.amount);

        return RLPEncoder.encodeList(ByteUtilLib.append(proof, amount));
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
        AMTreeProof proof;
    }

    function unmarshalCancelWithdrawal(RLPLib.RLPItem memory rlp) internal pure returns (CancelWithdrawal memory cancel) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cancel.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) cancel.update = unmarshalUpdate(r);
            else if(idx == 2) cancel.proof = unmarshalAMTreeProof(r);
            else {}

            idx++;
        }
    }

    function marshalCancelWithdrawal(CancelWithdrawal memory cancel) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(cancel.addr);
        bytes memory update = marshalUpdate(cancel.update);
        bytes memory proof = marshalAMTreeProof(cancel.proof);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(addr, update), proof));
    }

    struct BalanceUpdateChallenge {
        bool flag;
    }

    function unmarshalBalanceUpdateChallenge(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateChallenge memory buc) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) buc.flag = RLPDecoder.toBool(r);
            else {}

            idx++;
        }
    }

    function marshalBalanceUpdateChallenge(BalanceUpdateChallenge memory buc) internal pure returns (bytes memory) {
        bytes memory flag = RLPEncoder.encodeBool(buc.flag);
        return RLPEncoder.encodeList(flag);
    }

    struct BalanceUpdateProof {
        bool hasUp;
        Update update;
        bool hasPath;
        AMTreePath path;
    }

    function unmarshalBalanceUpdateProof(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateProof memory bup) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) bup.hasUp = RLPDecoder.toBool(r);
            else if(idx == 1) bup.update = unmarshalUpdate(r);
            else if(idx == 2) bup.hasPath = RLPDecoder.toBool(r);
            else if(idx == 3) bup.path = unmarshalAMTreePath(r);
            else {}

            idx++;
        }
    }

    function marshalBalanceUpdateProof(BalanceUpdateProof memory bup) internal pure returns (bytes memory) {
        bytes memory hasUp = RLPEncoder.encodeBool(bup.hasUp);
        bytes memory update = marshalUpdate(bup.update);
        bytes memory hasPath = RLPEncoder.encodeBool(bup.hasPath);
        bytes memory path = marshalAMTreePath(bup.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(hasUp, update), hasPath), path));
    }

    struct HubRoot {
        AMTreePathNode node;
        uint eon;
    }

    function unmarshalHubRoot(RLPLib.RLPItem memory rlp) internal pure returns (HubRoot memory hub) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint len = RLPDecoder.items(rlp);
        require(len == 2, "HubRoot unmarshal err");
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) hub.node = unmarshalAMTreePathNode(r);
            else if(idx == 1) hub.eon = RLPDecoder.toUint(r);
            else {}

            idx++;
        }
    }

    function marshalHubRoot(HubRoot memory hub) internal pure returns (bytes memory) {
        bytes memory node = marshalAMTreePathNode(hub.node);
        bytes memory eon = RLPEncoder.encodeUint(hub.eon);

        return RLPEncoder.encodeList(ByteUtilLib.append(node, eon));
    }

    function hubRootCommonVerify(HubRoot memory root) internal pure {
        require(root.node.offset == 0, "offset != 0");
        require(root.node.direction == ModelLib.Direction.DIRECTION_ROOT, "direction is not root");
    }

    struct ContractConstructArgs {
        HubRoot hubRoot;
        uint blocks;
    }

    function unmarshalContractConstructArgs(RLPLib.RLPItem memory rlp) internal pure returns (ContractConstructArgs memory args) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) args.blocks = RLPDecoder.toUint(r);
            else if(idx == 1) args.hubRoot = unmarshalHubRoot(r);
            else {}

            idx++;
        }
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

    struct Signature {
        byte v;
        bytes32 r;
        bytes32 s;
    }

    function unmarshalSignature(RLPLib.RLPItem memory rlp) internal pure returns (Signature memory sign) {
        rlp = RLPDecoder.toRLPItem(RLPLib.toData(rlp), true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                bytes memory bs = RLPLib.toData(r);
                sign.v = bs[0];
            } else if(idx == 1) {
                sign.r = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            } else if(idx == 2) {
                sign.s = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
             } else {}

            idx++;
        }
    }

    function marshalSignature(Signature memory sign) internal pure returns (bytes memory) {
        bytes memory tmp = new bytes(1);
        tmp[0] = sign.v;
        bytes memory v = RLPEncoder.encodeBytes(tmp);
        bytes memory r = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(sign.r));
        bytes memory s = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(sign.s));

        return RLPEncoder.encodeBytes(RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(v, r), s)));
    }

    struct OffchainTransaction {
        OffchainTransactionData offData;
        Signature sign;
    }

    function unmarshalOffchainTransaction(RLPLib.RLPItem memory rlp) internal pure returns (OffchainTransaction memory off) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) off.offData = unmarshalOffchainTransactionData(r);
            else if(idx == 1) off.sign = unmarshalSignature(r);//RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalOffchainTransaction(OffchainTransaction memory off) internal pure returns (bytes memory) {
        bytes memory offData = marshalOffchainTransactionData(off.offData);
        bytes memory sign = marshalSignature(off.sign);

        return RLPEncoder.encodeList(ByteUtilLib.append(offData, sign));
    }

    function hash4OffchainTransaction(OffchainTransaction memory self)  internal pure returns(bytes32) {
        bytes memory bs = marshalOffchainTransactionData(self.offData);
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
        MerklePath txPath;
        address fromAddr;
        bytes32 txHash;
    }

    function unmarshalCloseTransferDeliveryChallenge(RLPLib.RLPItem memory rlp) internal pure returns (CloseTransferDeliveryChallenge memory close) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.proof = unmarshalAMTreeProof(r);
            else if(idx == 1) close.txPath = unmarshalMerklePath(r);
            else if(idx == 2) close.fromAddr = RLPDecoder.toAddress(r);
            else if(idx == 3) close.txHash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else {}

            idx++;
        }
    }

    function marshalCloseTransferDeliveryChallenge(CloseTransferDeliveryChallenge memory close) internal pure returns (bytes memory) {
        bytes memory proof = marshalAMTreeProof(close.proof);
        bytes memory txPath = marshalMerklePath(close.txPath);
        bytes memory fromAddr = RLPEncoder.encodeAddress(lose.fromAddr);
        bytes memory txHash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(close.txHash));

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(proof, txPath), fromAddr), txHash));
    }

    struct BalanceUpdateChallengeStatus {
        BalanceUpdateProof proof;
        ChallengeStatus status;
    }

    function unmarshalBalanceUpdateChallengeStatus(RLPLib.RLPItem memory rlp) internal pure returns (BalanceUpdateChallengeStatus memory challengeStatus) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) challengeStatus.proof = unmarshalBalanceUpdateProof(r);
            else if(idx == 1) challengeStatus.status = unmarshalChallengeStatus(r);
            else {}

            idx++;
        }
    }

    function marshalBalanceUpdateChallengeStatus(BalanceUpdateChallengeStatus memory challengeStatus) internal pure returns (bytes memory) {
        bytes memory proof = marshalBalanceUpdateProof(challengeStatus.proof);
        bytes memory status = marshalChallengeStatus(challengeStatus.status);

        return RLPEncoder.encodeList(ByteUtilLib.append(proof, status));
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
        uint latestEon;
    }

    function marshalContractHubInfo(ContractHubInfo memory chi) internal pure returns (bytes memory) {
        bytes memory startBlockNum = RLPEncoder.encodeUint(chi.startBlockNum);
        bytes memory hubAddress = RLPEncoder.encodeString(chi.hubAddress);
        bytes memory blocksPerEon = RLPEncoder.encodeUint(chi.blocksPerEon);
        bytes memory latestEon = RLPEncoder.encodeUint(chi.latestEon);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(startBlockNum, hubAddress), blocksPerEon), latestEon));
    }

    struct ContractReturn {
       bool hasVal;
       bytes payload;
    }

    function unmarshalContractReturn(RLPLib.RLPItem memory rlp) internal pure returns (ContractReturn memory cr) {
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cr.hasVal = RLPDecoder.toBool(r);
            else if(idx == 1) cr.payload = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshalContractReturn(ContractReturn memory cr) internal pure returns (bytes memory) {
        bytes memory hasVal = RLPEncoder.encodeBool(cr.hasVal);
        bytes memory payload = RLPEncoder.encodeBytes(cr.payload);

        return RLPEncoder.encodeList(ByteUtilLib.append(hasVal, payload));
    }
}
