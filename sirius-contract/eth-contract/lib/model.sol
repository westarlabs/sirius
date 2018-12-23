pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
import "./safe_math.sol";
import "./byte_util.sol";

library WithdrawalStatusTypeLib {

    enum WithdrawalStatusType {
        INIT,
        CANCEL,
        CONFIRMED
    }

    function unmarshal(bytes memory data) internal pure returns (WithdrawalStatusTypeLib.WithdrawalStatusType stat) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 1) {
                    return WithdrawalStatusTypeLib.WithdrawalStatusType.CANCEL;
                } else if(tmp == 2) {
                    return WithdrawalStatusTypeLib.WithdrawalStatusType.CONFIRMED;
                }
            } else {}

            idx++;
        }

        return WithdrawalStatusTypeLib.WithdrawalStatusType.INIT;
    }

    function marshal(WithdrawalStatusTypeLib.WithdrawalStatusType stat) internal pure returns (bytes memory) {
        uint tmp;
        if(stat == WithdrawalStatusTypeLib.WithdrawalStatusType.CANCEL) {
            tmp = 1;
        } else if(stat == WithdrawalStatusTypeLib.WithdrawalStatusType.CONFIRMED) {
            tmp = 2;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }
}

library WithdrawalLib {

    using SafeMath for uint;

    struct Withdrawal {
        address addr;
        InitiateWithdrawalRequestLib.WithdrawalInfo info;
        WithdrawalStatusTypeLib.WithdrawalStatusType stat;
        bool isVal;
    }

    struct WithdrawalMeta {
        uint total;
        address[] addrs;
        mapping(address => Withdrawal) withdrawals;
    }
}

library RecoveryMetaLib {
    struct RecoveryMeta {
        bool isVal;
    }
}

library DepositLib {

    using SafeMath for uint;

    struct Deposit {
        bool hasVal;
        address addr;
        uint amount;
    }

    struct DepositMeta {
        uint total;
        mapping(address => uint) deposits;
    }

    function add(DepositMeta storage self, address addr, uint amount) internal {
        self.deposits[addr] = SafeMath.add(self.deposits[addr], amount);
        self.total = SafeMath.add(self.total, amount);
    }
}

library BalanceLib {
    struct Balance {
        uint eon;
        DepositLib.DepositMeta depositMeta;
        WithdrawalLib.WithdrawalMeta withdrawalMeta;
        HubRootLib.HubRoot root;
        mapping(address => BalanceUpdateChallengeStatusLib.BalanceUpdateChallengeStatus) balanceChallenges;
        mapping(string => TransferDeliveryChallengeLib.TransferDeliveryChallenge) transferChallenges;
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

    function hubRoot2AugmentedMerkleTreeNode(HubRootLib.HubRoot memory root) internal pure returns (AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle) {
        merkle.offset = root.node.offset;
        merkle.allotment = root.node.allotment;
        merkle.node = root.node.info;
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

    function verifySig(ParticipantLib.Participant memory participant, UpdateLib.Update memory update) internal pure returns(bool flag) {
        //TODO
        participant;
        update;
        return true;
    }

    function verifyHubSig(UpdateLib.Update memory update, bytes memory hubPK) internal pure returns(bool flag) {
        hubPK;
        update;
        return true;
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

library AugmentedMerkleTreeLib {
    function verifyMembershipProof(AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory root, AugmentedMerklePathLib.AugmentedMerklePath memory path) internal pure returns(bool flag) {
        //TODO
        root;
        path;
        return true;
    }
}

library AugmentedMerklePathLib {
    struct AugmentedMerklePath {
        uint eon;
        AugmentedMerklePathNodeLib.AugmentedMerklePathNode[] nodes;
    }

    function leafNode(AugmentedMerklePathLib.AugmentedMerklePath memory self) internal pure returns(AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf) {
        uint idx = SafeMath.sub(self.nodes.length, 1);

        AugmentedMerklePathNodeLib.AugmentedMerklePathNode memory node = self.nodes[idx];
        leaf = node.node;
    }

    function unmarshal(bytes memory data) internal pure returns (AugmentedMerklePathLib.AugmentedMerklePath memory path) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) path.eon = RLPDecoder.toUint(r);
            else if(idx == 1) {
                uint len = RLPDecoder.items(r);
                AugmentedMerklePathNodeLib.AugmentedMerklePathNode[] memory tmp = new AugmentedMerklePathNodeLib.AugmentedMerklePathNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = AugmentedMerklePathNodeLib.unmarshal(RLPLib.toData(t));
                    i++;
                }
                path.nodes = tmp;
            } else {}
        }
    }

    function marshal(AugmentedMerklePathLib.AugmentedMerklePath memory path) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(path.eon);

        bytes memory data;
        AugmentedMerklePathNodeLib.AugmentedMerklePathNode[] memory nodes = path.nodes;
        for(uint i=0;i<nodes.length;i++) {
            data = ByteUtilLib.append(data, AugmentedMerklePathNodeLib.marshal(nodes[i]));
        }

        data = RLPEncoder.encodeList(data);

        return RLPEncoder.encodeList(ByteUtilLib.append(eon, data));
    }
}

library InitiateWithdrawalRequestLib {
    struct WithdrawalInfo {
        AugmentedMerklePathLib.AugmentedMerklePath path;
        uint amount;
    }

    function marshal(InitiateWithdrawalRequestLib.WithdrawalInfo memory init) internal pure returns (bytes memory) {
        bytes memory path = AugmentedMerklePathLib.marshal(init.path);
        bytes memory amount = RLPEncoder.encodeUint(init.amount);

        return RLPEncoder.encodeList(ByteUtilLib.append(path, amount));
    }

    function unmarshal(bytes memory data) internal pure returns (InitiateWithdrawalRequestLib.WithdrawalInfo memory init) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) init.path = AugmentedMerklePathLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) init.amount = RLPDecoder.toUint(r);
            else {}
        }
    }
}

library ParticipantLib {
    struct Participant {
        address addr;
        bytes publicKey;
    }

    function verifyParticipant(ParticipantLib.Participant memory participant) internal pure returns(bool flag){
        //TODO
        participant;//publicKey can comput addr
        return true;
    }

    function unmarshal(bytes memory data) internal pure returns (ParticipantLib.Participant memory participant) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);

        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) participant.addr = RLPDecoder.toAddress(r);
            else if(idx == 1) participant.publicKey = RLPLib.toData(r);
            else {}

            idx++;
        }
    }

    function marshal(ParticipantLib.Participant memory participant) internal pure returns (bytes memory) {
        bytes memory addr = RLPEncoder.encodeAddress(participant.addr);
        return RLPEncoder.encodeList(ByteUtilLib.append(addr, RLPEncoder.encodeBytes(participant.publicKey)));
    }
}

library CancelWithdrawalRequestLib {
    struct CancelWithdrawalRequest {
        ParticipantLib.Participant participant;
        UpdateLib.Update update;
        AugmentedMerklePathLib.AugmentedMerklePath merklePath;
    }

    function unmarshal(bytes memory data) internal pure returns (CancelWithdrawalRequestLib.CancelWithdrawalRequest memory cancel) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) cancel.participant = ParticipantLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) cancel.update = UpdateLib.unmarshal(RLPLib.toData(r));
            else if(idx == 2) cancel.merklePath = AugmentedMerklePathLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(CancelWithdrawalRequestLib.CancelWithdrawalRequest memory cancel) internal pure returns (bytes memory) {
        bytes memory participant = ParticipantLib.marshal(cancel.participant);
        bytes memory update = UpdateLib.marshal(cancel.update);
        bytes memory merklePath = AugmentedMerklePathLib.marshal(cancel.merklePath);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(participant, update), merklePath));
    }
}

library BalanceUpdateProofLib {
    struct BalanceUpdateProof {
        bool hasPath;
        AugmentedMerklePathLib.AugmentedMerklePath path;
        bool hasUp;
        UpdateLib.Update update;
    }

    function marshal(BalanceUpdateProofLib.BalanceUpdateProof memory proof) internal pure returns (bytes memory) {
        bytes memory hasPath = RLPEncoder.encodeBool(proof.hasPath);
        bytes memory path = AugmentedMerklePathLib.marshal(proof.path);
        bytes memory hasUp = RLPEncoder.encodeBool(proof.hasUp);
        bytes memory update = UpdateLib.marshal(proof.update);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(hasPath, path), hasUp), update));
    }

    function unmarshal(bytes memory data) internal pure returns (BalanceUpdateProofLib.BalanceUpdateProof memory proof) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) proof.hasPath = RLPDecoder.toBool(r);
            else if(idx == 1) proof.path = AugmentedMerklePathLib.unmarshal(RLPLib.toData(r));
            else if(idx == 2) proof.hasUp = RLPDecoder.toBool(r);
            else if(idx == 3) proof.update = UpdateLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }
}

library BalanceUpdateChallengeLib {
    struct BalanceUpdateChallenge {
        BalanceUpdateProofLib.BalanceUpdateProof proof;
        bytes publicKey;
    }

    function marshal(BalanceUpdateChallengeLib.BalanceUpdateChallenge memory challenge) internal pure returns (bytes memory) {
        bytes memory proof = BalanceUpdateProofLib.marshal(challenge.proof);
        bytes memory publicKey = RLPEncoder.encodeBytes(challenge.publicKey);

        return RLPEncoder.encodeList(ByteUtilLib.append(proof, publicKey));
    }

    function unmarshal(bytes memory data) internal pure returns (BalanceUpdateChallengeLib.BalanceUpdateChallenge memory challenge) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) challenge.proof = BalanceUpdateProofLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) challenge.publicKey = RLPLib.toData(r);
            else {}
        }
    }
}

library ChallengeStatusLib {
    enum ChallengeStatus {
        OPEN,
        CLOSE
    }

    function unmarshal(bytes memory data) internal pure returns (ChallengeStatusLib.ChallengeStatus status) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint tmp = RLPDecoder.toUint(r);
                if(tmp == 0) {
                    status = ChallengeStatusLib.ChallengeStatus.OPEN;
                } else if(tmp == 1) {
                    status = ChallengeStatusLib.ChallengeStatus.CLOSE;
                }
            } else {}

            idx++;
        }
    }

    function marshal(ChallengeStatusLib.ChallengeStatus dir) internal pure returns (bytes memory) {
        uint tmp;
        if(dir == ChallengeStatusLib.ChallengeStatus.OPEN) {
            tmp = 0;
        } else if(dir == ChallengeStatusLib.ChallengeStatus.CLOSE) {
            tmp = 1;
        }

        return RLPEncoder.encodeList(RLPEncoder.encodeUint(tmp));
    }
}

library BalanceUpdateChallengeStatusLib {
    struct BalanceUpdateChallengeStatus {
        BalanceUpdateChallengeLib.BalanceUpdateChallenge challenge;
        ChallengeStatusLib.ChallengeStatus status;
        bool isVal;
    }

    function unmarshal(bytes memory data) internal pure returns (BalanceUpdateChallengeStatusLib.BalanceUpdateChallengeStatus memory challengeStatus) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) challengeStatus.challenge = BalanceUpdateChallengeLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) challengeStatus.status = ChallengeStatusLib.unmarshal(RLPLib.toData(r));
            else {}
        }

        challengeStatus.isVal = true;
    }

    function marshal(BalanceUpdateChallengeStatusLib.BalanceUpdateChallengeStatus memory challengeStatus) internal pure returns (bytes memory) {
        bytes memory challenge = BalanceUpdateChallengeLib.marshal(challengeStatus.challenge);
        bytes memory status = ChallengeStatusLib.marshal(challengeStatus.status);

        return RLPEncoder.encodeList(ByteUtilLib.append(challenge, status));
    }
}

library OffchainTransactionLib {
    struct OffchainTransaction {
        uint eon;
        address fr;
        address to;
        uint amount;
        uint timestamp;
        SignatureLib.Signature sign;
    }

    function unmarshal(bytes memory data) internal pure returns (OffchainTransactionLib.OffchainTransaction memory off) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) off.eon = RLPDecoder.toUint(r);
            else if(idx == 1) off.fr = RLPDecoder.toAddress(r);
            else if(idx == 2) off.to = RLPDecoder.toAddress(r);
            else if(idx == 3) off.amount = RLPDecoder.toUint(r);
            else if(idx == 4) off.timestamp = RLPDecoder.toUint(r);
            else if(idx == 5) off.sign = SignatureLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(OffchainTransactionLib.OffchainTransaction memory off) internal pure returns (bytes memory) {
        bytes memory eon = RLPEncoder.encodeUint(off.eon);
        bytes memory fr = RLPEncoder.encodeAddress(off.fr);
        bytes memory to = RLPEncoder.encodeAddress(off.to);
        bytes memory amount = RLPEncoder.encodeUint(off.amount);
        bytes memory timestamp = RLPEncoder.encodeUint(off.timestamp);
        bytes memory sign = SignatureLib.marshal(off.sign);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(eon, fr), to), amount), timestamp), sign));
    }

    function hash(OffchainTransactionLib.OffchainTransaction memory off) internal pure returns(string memory) {
        //TODO
        off;
        return "";
    }
}

library MerkleTreeNodeLib {
    struct MerkleTreeNode {
        bytes32 hash;
        OffchainTransactionLib.OffchainTransaction tran;
    }

    function unmarshal(bytes memory data) internal pure returns (MerkleTreeNodeLib.MerkleTreeNode memory node) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) node.hash = ByteUtilLib.bytesToBytes32(RLPLib.toData(r));
            else if(idx == 1) node.tran = OffchainTransactionLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(MerkleTreeNodeLib.MerkleTreeNode memory node) internal pure returns (bytes memory) {
        bytes memory hash = RLPEncoder.encodeBytes(ByteUtilLib.bytes32ToBytes(node.hash));
        bytes memory tran = OffchainTransactionLib.marshal(node.tran);

        return RLPEncoder.encodeList(ByteUtilLib.append(hash, tran));
    }
}

library MerklePathNodeLib {
    struct MerklePathNode {
        MerkleTreeNodeLib.MerkleTreeNode node;
        MerklePathDirectionLib.MerklePathDirection dir;
    }

    function unmarshal(bytes memory data) internal pure returns (MerklePathNodeLib.MerklePathNode memory pathNode) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) pathNode.node = MerkleTreeNodeLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) pathNode.dir = MerklePathDirectionLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(MerklePathNodeLib.MerklePathNode memory pathNode) internal pure returns (bytes memory) {
        bytes memory node = MerkleTreeNodeLib.marshal(pathNode.node);
        bytes memory dir = MerklePathDirectionLib.marshal(pathNode.dir);

        return RLPEncoder.encodeList(ByteUtilLib.append(node, dir));
    }
}

library MerklePathLib {
    struct MerklePath {
        MerklePathNodeLib.MerklePathNode[] nodes;
    }

    function unmarshal(bytes memory data) internal pure returns (MerklePathLib.MerklePath memory path) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) {
                uint len = RLPDecoder.items(r);
                MerklePathNodeLib.MerklePathNode[] memory tmp = new MerklePathNodeLib.MerklePathNode[](len);
                RLPLib.Iterator memory it2 = RLPDecoder.iterator(r);
                uint i;
                while(RLPDecoder.hasNext(it2)) {
                    RLPLib.RLPItem memory t = RLPDecoder.next(it2);
                    tmp[i] = MerklePathNodeLib.unmarshal(RLPLib.toData(t));
                    i++;
                }
                path.nodes = tmp;
            } else {}
        }
    }

    function marshal(MerklePathLib.MerklePath memory path) internal pure returns (bytes memory) {
        bytes memory data;
        MerklePathNodeLib.MerklePathNode[] memory nodes = path.nodes;
        for(uint i=0;i<nodes.length;i++) {
            data = ByteUtilLib.append(data, MerklePathNodeLib.marshal(nodes[i]));
        }

        data = RLPEncoder.encodeList(data);

        return RLPEncoder.encodeList(data);
    }

    function leafNode(MerklePathLib.MerklePath memory path) internal pure returns(MerklePathNodeLib.MerklePathNode memory) {
        uint len = SafeMath.sub(path.nodes.length, 1);
        return path.nodes[len];
    }
}

library MerkleTreeLib {
    function verifyMembershipProof(bytes32 root, MerklePathLib.MerklePath memory path) internal pure returns(bool flag) {
        root;
        path;
        //TODO
        return true;
    }
}

library OpenTransferDeliveryChallengeRequestLib {
    struct OpenTransferDeliveryChallengeRequest {
        OffchainTransactionLib.OffchainTransaction tran;
        UpdateLib.Update update;
        MerklePathLib.MerklePath path;
    }

    function unmarshal(bytes memory data) internal pure returns (OpenTransferDeliveryChallengeRequestLib.OpenTransferDeliveryChallengeRequest memory open) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) open.tran = OffchainTransactionLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) open.update = UpdateLib.unmarshal(RLPLib.toData(r));
            else if(idx == 2) open.path = MerklePathLib.unmarshal(RLPLib.toData(r));
            else {}
        }
    }

    function marshal(OpenTransferDeliveryChallengeRequestLib.OpenTransferDeliveryChallengeRequest memory open) internal pure returns (bytes memory) {
        bytes memory tran = OffchainTransactionLib.marshal(open.tran);
        bytes memory update = UpdateLib.marshal(open.update);
        bytes memory path = MerklePathLib.marshal(open.path);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(tran, update), path));
    }
}

library CloseTransferDeliveryChallengeRequestLib {
    struct CloseTransferDeliveryChallengeRequest {
        AugmentedMerklePathLib.AugmentedMerklePath merklePath;
        UpdateLib.Update update;
        MerklePathLib.MerklePath path;
        bytes fromPublicKey;
    }

    function unmarshal(bytes memory data) internal pure returns (CloseTransferDeliveryChallengeRequestLib.CloseTransferDeliveryChallengeRequest memory close) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);
            if(idx == 0) close.merklePath = AugmentedMerklePathLib.unmarshal(RLPLib.toData(r));
            else if(idx == 1) close.update = UpdateLib.unmarshal(RLPLib.toData(r));
            else if(idx == 2) close.path = MerklePathLib.unmarshal(RLPLib.toData(r));
            else if(idx == 3) close.fromPublicKey = RLPLib.toData(r);
            else {}
        }
    }

    function marshal(CloseTransferDeliveryChallengeRequestLib.CloseTransferDeliveryChallengeRequest memory close) internal pure returns (bytes memory) {
        bytes memory merklePath = AugmentedMerklePathLib.marshal(close.merklePath);
        bytes memory update = UpdateLib.marshal(close.update);
        bytes memory path = MerklePathLib.marshal(close.path);
        bytes memory fromPublicKey = RLPEncoder.encodeBytes(close.fromPublicKey);

        return RLPEncoder.encodeList(ByteUtilLib.append(ByteUtilLib.append(ByteUtilLib.append(merklePath, update), path), fromPublicKey));
    }
}

library TransferDeliveryChallengeLib {
    struct TransferDeliveryChallenge {
        OffchainTransactionLib.OffchainTransaction tran;
        UpdateLib.Update update;
        MerklePathLib.MerklePath path;
        ChallengeStatusLib.ChallengeStatus stat;
        bool isVal;
    }
}