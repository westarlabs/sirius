pragma solidity ^0.5.1;

import "./lib/model.sol";
import "./lib/safe_math.sol";

interface Sirius {
    function deposit() external payable;
    function commit(bytes calldata data) external;
    function initiateWithdrawal(bytes calldata data) external;
    function cancelWithdrawal(bytes calldata data) external;
    function openBalanceUpdateChallenge(bytes calldata data) external;
    function closeBalanceUpdateChallenge(bytes calldata data) external;
    function openTransferDeliveryChallenge(bytes calldata data) external;
    function closeTransferDeliveryChallenge(bytes calldata data) external;
    function recoverFunds(bytes calldata data) external;
    function getLatestRoot() external returns (bytes memory);
    function getCurrentEon() external returns (uint);
    function isRecoveryMode() external returns (bool);
    function test() external returns (bool);
}

contract SiriusService is Sirius {
    address private owner = msg.sender;
    bool private recoveryMode = false;
    uint private startHeight = block.number;
    uint private blocksPerEon = 4;
    uint private balanceSize;
    GlobleLib.Balance[3] balances;
    mapping(address => GlobleLib.Deposit) private all;
    bytes private hubPK;
    mapping(address => GlobleLib.RecoveryMeta) private recoverys;

    using SafeMath for uint;

    event DepositEvent(address indexed addr, uint value);

    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    modifier notRecoveryMode() {
        require(!recoveryMode);
        _;
    }

    modifier recovery() {
        require(!recoveryMode);

        //current eon
        uint tmp = SafeMath.sub(block.number, startHeight);
        uint newEon = SafeMath.div(tmp, blocksPerEon);

        if (newEon > balances[0].eon) {
            uint tmpEon = SafeMath.add(balances[0].eon, 1);
            if (newEon == tmpEon) {// init eon
                GlobleLib.DepositMeta memory depositMeta;
                GlobleLib.WithdrawalMeta memory withdrawalMeta;
                ModelLib.HubRoot memory root;
                GlobleLib.Balance memory latest = GlobleLib.Balance(newEon, depositMeta, withdrawalMeta, root, false);
                checkBalances(latest);
            } else {//recovery
                if (!balances[0].hasRoot) {
                    uint tmp2 = SafeMath.add(SafeMath.mul(blocksPerEon, newEon), SafeMath.div(blocksPerEon, 4));
                    if (tmp > tmp2) {
                        recoveryMode = true;
                        //TODO: add event
                    }
                }
            }
        }

        require(!recoveryMode);
        _;
    }

    function deposit() external payable recovery {
        require(msg.value > 0);
        GlobleLib.deposit(balances[0].depositMeta, msg.sender, msg.value);

        GlobleLib.Deposit memory d = all[msg.sender];
        d.amount = SafeMath.add(d.amount, msg.value);
        d.hasVal = true;
        all[msg.sender] = d;

        emit DepositEvent(msg.sender, msg.value);
    }

    function commit(bytes calldata data) external recovery {
        ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(data);
        require(!balances[0].hasRoot);
        require(root.eon >= 0);
        require(balances[0].eon == root.eon);
        require(root.node.allotment >= 0);
        uint tmp = SafeMath.add(balances[1].root.node.allotment, balances[1].depositMeta.total);
        uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalMeta.total);
        require(allotmentTmp >= 0 && allotmentTmp == root.node.allotment);
        balances[0].root = root;
        balances[0].hasRoot = true;
        //TODO:add event
    }

    function checkBalances(GlobleLib.Balance memory latest) private {
        uint i = (balances.length - 1);
        for (; i >= 0; i--) {
            if (i == 0) {
                balances[i] = latest;
                //TODO: add event
            }else if (balances[i - 1].hasRoot)
                balances[i] = balances[i - 1];
        }
    }

    function initiateWithdrawal(bytes calldata data) external recovery {
        ModelLib.WithdrawalInfo memory init = ModelLib.unmarshalWithdrawalInfo(data);
        require(init.amount > 0);

        uint currentEon = currentEon();
        require(init.path.eon >= 0 && init.path.eon == currentEon);

        uint len = init.path.nodes.length;
        require(len > 0);
        // uint idx = SafeMath.sub(len, 1);
        // AccountInfoLib.AccountInfo memory account = init.path.nodes[idx].node.account;

        // bytes memory bs = ByteUtilLib.bytes32ToBytes(init.addr);
        // address addr = ByteUtilLib.bytesToAddress(bs);
        require(init.addr == msg.sender);

        bool processingFlag = processingWithdrawal(init.addr);
        require(!processingFlag);

        ModelLib.HubRoot memory latestRoot = latestRoot();
        //AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(latestRoot.node, init.path);
        require(proofFlag);

        require(init.path.leaf.allotment >= init.amount);

        GlobleLib.Withdrawal memory with;
        with.info = init;
        with.isVal = true;
        with.stat = GlobleLib.WithdrawalStatusType.INIT;

        balances[0].withdrawalMeta.addrs.push(init.addr);
        // balances[0].withdrawalMeta.withdrawals[addr] = with;//TODO:change path to bytes
        balances[0].withdrawalMeta.total += init.amount;
    }

    function processingWithdrawal(address addr) private view returns (bool flag) {
        flag = true;
        for(uint i=0;i<balances.length;i++) {
            if(balances[i].withdrawalMeta.withdrawals[addr].isVal) {
                GlobleLib.Withdrawal memory with = balances[i].withdrawalMeta.withdrawals[addr];

                if(with.stat == GlobleLib.WithdrawalStatusType.CANCEL || with.stat == GlobleLib.WithdrawalStatusType.CONFIRMED) {
                    flag = false;
                }

                break;
            }
        }
    }

    function cancelWithdrawal(bytes calldata data) external recovery {
        ModelLib.CancelWithdrawal memory cancel = ModelLib.unmarshalCancelWithdrawal(data);
        uint currentEon = currentEon();
        require(cancel.update.upData.eon >= 0 && cancel.update.upData.eon == currentEon);

        bool participantFlag = ModelLib.verifyParticipant(cancel.participant);
        require(participantFlag);

        bool signFlag = ModelLib.verifySig4Update(cancel.participant, cancel.update);
        require(signFlag);

        ModelLib.HubRoot memory latestRoot = latestRoot();
        // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(latestRoot.node, cancel.path);
        require(proofFlag);

        bytes memory bs = ByteUtilLib.bytes32ToBytes(cancel.path.leaf.nodeInfo.addressHash);
        address addr = ByteUtilLib.bytesToAddress(bs);

        GlobleLib.Withdrawal storage with = balances[0].withdrawalMeta.withdrawals[addr];
        if(with.isVal) {
            if(with.stat == GlobleLib.WithdrawalStatusType.INIT) {
                //AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(cancel.merklePath);
                // AccountInfoLib.AccountInfo memory account = leaf.account;

                uint tmp = SafeMath.sub(SafeMath.add(cancel.path.leaf.allotment, cancel.update.upData.receiveAmount), cancel.update.upData.sendAmount);
                if (with.info.amount > tmp) {
                    with.stat = GlobleLib.WithdrawalStatusType.CANCEL;
                    balances[0].withdrawalMeta.withdrawals[addr] = with;
                }
            }
        }
    }

    function openBalanceUpdateChallenge(bytes calldata data) external recovery {
        ModelLib.BalanceUpdateChallenge memory challenge = ModelLib.unmarshalBalanceUpdateChallenge(data);
        require(challenge.proof.hasPath || challenge.proof.hasUp);

        if(challenge.proof.hasPath) {
            uint tmpEon =  challenge.proof.proof.leaf.nodeInfo.update.upData.eon;
            require(tmpEon > 0);
            require(balances[1].hasRoot);
            require(tmpEon == balances[1].eon);

            ModelLib.HubRoot memory root = balances[1].root;
            //AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(root);
            //bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, challenge.proof.path);
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(root.node, challenge.proof.proof.path);
            require(proofFlag);
        } else {
            GlobleLib.Deposit memory d = all[msg.sender];
            require(!d.hasVal);
        }

        if(challenge.proof.hasUp) {
            ModelLib.Update memory up = challenge.proof.update;

            uint tmpEon = up.upData.eon;
            require(tmpEon > 0);
            require(tmpEon == balances[1].eon);

            bool signFlag = ModelLib.verifyHubSig4Update(up, hubPK);
            require(signFlag);
        } else {

        }

        ModelLib.BalanceUpdateChallengeStatus memory cs;
        cs.challenge = challenge;
        cs.status = ModelLib.ChallengeStatus.OPEN;

        // balances[0].balanceChallenges[msg.sender] = cs;//TODO:change path to bytes
    }

    function closeBalanceUpdateChallenge(bytes calldata data) external recovery {
        ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(data);

        ModelLib.HubRoot memory root = balances[1].root;
        //AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(root);
        // bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, proof.path);
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(root.node, close.proof.path);
        require(proofFlag);

        // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(proof.path);
        // AccountInfoLib.AccountInfo memory account = leaf.account;
        bytes memory bs = ByteUtilLib.bytes32ToBytes(close.proof.leaf.nodeInfo.addressHash);
        address addr = ByteUtilLib.bytesToAddress(bs);

        ModelLib.BalanceUpdateChallengeStatus storage stat = balances[0].balanceChallenges[addr];
        require(stat.isVal);
        if(stat.status == ModelLib.ChallengeStatus.OPEN) {
            uint d = balances[1].depositMeta.deposits[addr];
            if(close.update.upData.sendAmount == 0 && close.update.upData.receiveAmount == 0) {
                if(d > 0) {
                    require(d <= close.proof.leaf.allotment);
                }
            } else {
                ModelLib.Participant memory participant;
                participant.publicKey = ByteUtilLib.bytes32ToBytes(stat.challenge.publicKey);
                bool signFlag = ModelLib.verifySig4Update(participant, close.update);
                require(signFlag);
                bool signFlag2 = ModelLib.verifyHubSig4Update(close.update, hubPK);
                require(signFlag2);
                require(close.update.upData.version >= stat.challenge.proof.update.upData.version);
            }

            GlobleLib.Withdrawal memory w = balances[1].withdrawalMeta.withdrawals[addr];
            uint preAllotment = 0;
            if(stat.challenge.proof.hasPath) {
                // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory tmp = AugmentedMerklePathLib.leafNode(stat.challenge.proof.path);
                // preAllotment = tmp.account.allotment;

                preAllotment = stat.challenge.proof.proof.leaf.allotment;
            }

            uint t1 = SafeMath.add(close.update.upData.receiveAmount, preAllotment);
            t1 = SafeMath.add(t1, d);
            t1 = SafeMath.sub(t1, w.info.amount);
            uint t2 = close.update.upData.sendAmount;
            uint allotment = SafeMath.sub(t1, t2);
            require(allotment == close.proof.leaf.allotment);

            //TODO: require(close.update == close.proof.leaf.nodeInfo.update);

            stat.status == ModelLib.ChallengeStatus.CLOSE;
            balances[0].balanceChallenges[addr] = stat;
        }
    }

    function openTransferDeliveryChallenge(bytes calldata data) external recovery {
        ModelLib.OpenTransferDeliveryChallengeRequest memory open = ModelLib.unmarshalOpenTransferDeliveryChallengeRequest(data);
        require(open.update.upData.eon >= 0);
        require(open.update.upData.eon == balances[1].eon);

        bool signFlag = ModelLib.verifyHubSig4Update(open.update, hubPK);
        require(signFlag);

        bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(open.update.upData.root, open.path);
        require(verifyFlag);

        // MerklePathNodeLib.MerklePathNode memory leaf = MerklePathLib.leafNode(open.path);
        // require(OffchainTransactionLib.equals(open.tran, leaf.node.tran));

        require(ModelLib.hash4OffchainTransactionData(open.tran.offData) == open.path.nodes[0].nodeHash);

        GlobleLib.TransferDeliveryChallenge memory challenge;
        challenge.tran = open.tran;
        challenge.update = open.update;
        challenge.path = open.path;
        challenge.stat = ModelLib.ChallengeStatus.OPEN;
        challenge.isVal = true;

        // bytes32 hash = OffchainTransactionLib.hash(open.tran);
        // string memory key = string(ByteUtilLib.bytes32ToBytes(hash));
        // balances[0].transferChallenges[key] = challenge;//TODO:change path to bytes
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external recovery {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(data);

        ModelLib.HubRoot memory latestRoot = latestRoot();
        // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        // bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, close.merklePath);
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(latestRoot.node, close.proof.path);
        require(proofFlag);

        ModelLib.MerklePathNode memory leaf = ModelLib.leaf4MerklePath(close.txPath);
        string memory key = string(ByteUtilLib.bytes32ToBytes(leaf.nodeHash));
        GlobleLib.TransferDeliveryChallenge memory challenge = balances[0].transferChallenges[key];
        require(challenge.isVal);

        if(challenge.stat == ModelLib.ChallengeStatus.OPEN) {
            ModelLib.Participant memory participant;
            participant.publicKey = ByteUtilLib.bytes32ToBytes(close.fromPublicKey);
            bool signFlag = ModelLib.verifySig4Update(participant, close.update);
            require(signFlag);

            // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf2 = AugmentedMerklePathLib.leafNode(close.merklePath);
            // require(UpdateLib.equals(leaf2.account.update, close.update));

            //TODO:require(close.proof.leaf.nodeInfo.update == close.update);

            bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(close.update.upData.root, close.txPath);
            require(verifyFlag);

            challenge.stat = ModelLib.ChallengeStatus.CLOSE;
            // balances[0].transferChallenges[key] = challenge;//TODO:change path to bytes
        }
    }

    function recoverFunds(bytes calldata data) external recovery {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(data);

        // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(path);

        require(ByteUtilLib.bytesToAddress(ByteUtilLib.bytes32ToBytes(proof.leaf.nodeInfo.addressHash)) == msg.sender);

        require(balances[1].eon == proof.leaf.nodeInfo.update.upData.eon);

        ModelLib.HubRoot memory latestRoot = latestRoot();
        // AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        // bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, path);
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(latestRoot.node, proof.path);
        require(proofFlag);

        uint amount = SafeMath.add(proof.leaf.allotment, balances[0].depositMeta.deposits[msg.sender]);
        amount = SafeMath.add(amount, balances[1].depositMeta.deposits[msg.sender]);
        require(amount > 0);

        GlobleLib.RecoveryMeta memory r = recoverys[msg.sender];
        require(!r.isVal);
        r.isVal = true;
        recoverys[msg.sender] = r;
        msg.sender.transfer(amount);
    }

    function getLatestRoot() external recovery returns (bytes memory) {
        return ModelLib.marshalHubRoot(latestRoot());
    }

    function latestRoot() private view returns (ModelLib.HubRoot memory) {
        return balances[0].root;
    }

    function getCurrentEon() external recovery returns (uint) {
        return currentEon();
    }

    function currentEon() private view returns (uint) {
        return balances[0].eon;
    }

    function isRecoveryMode() external recovery returns (bool) {
        return recoveryMode;
    }

    function test() external returns (bool) {
        return true;
    }
}