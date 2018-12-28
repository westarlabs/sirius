pragma solidity ^0.5.1;

import "./lib/model.sol";
import "./lib/safe_math.sol";

interface Sirius {
    function deposit(uint a) external payable;
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
}

contract SiriusService is Sirius {
    address private owner = msg.sender;
    bool private recoveryMode = false;
    uint private startHeight = block.number;
    uint private blocksPerEon = 4;
    uint private balanceSize;
    BalanceLib.Balance[3] balances;
    mapping(address => DepositLib.Deposit) private all;
    bytes private hubPK;
    mapping(address => RecoveryMetaLib.RecoveryMeta) private recoverys;

    using SafeMath for uint;
    using DepositLib for DepositLib.DepositMeta;

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
        //current eon
        uint tmp = SafeMath.sub(block.number, startHeight);
        uint newEon = SafeMath.div(tmp, blocksPerEon);

        if (newEon > balances[0].eon) {
            uint tmpEon = SafeMath.add(balances[0].eon, 1);
            if (newEon == tmpEon) {// init eon
                DepositLib.DepositMeta memory depositMeta;
                WithdrawalLib.WithdrawalMeta memory withdrawalMeta;
                HubRootLib.HubRoot memory root;
                BalanceLib.Balance memory latest = BalanceLib.Balance(newEon, depositMeta, withdrawalMeta, root, false);
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

    function deposit(uint a) external payable recovery {
        require(msg.value > 0);
        require(a > 0);
        require(msg.value == a);
        DepositLib.add(balances[0].depositMeta, msg.sender, msg.value);

        DepositLib.Deposit memory d = all[msg.sender];
        d.amount = SafeMath.add(d.amount, msg.value);
        d.hasVal = true;
        all[msg.sender] = d;

        emit DepositEvent(msg.sender, msg.value);
    }

    function commit(bytes calldata data) external recovery {
        HubRootLib.HubRoot memory root = HubRootLib.unmarshal(data);
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

    function checkBalances(BalanceLib.Balance memory latest) private {
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
        InitiateWithdrawalRequestLib.WithdrawalInfo memory init = InitiateWithdrawalRequestLib.unmarshal(data);
        require(init.amount > 0);

        uint currentEon = currentEon();
        require(init.path.eon >= 0 && init.path.eon == currentEon);

        uint len = init.path.nodes.length;
        require(len > 0);
        uint idx = SafeMath.sub(len, 1);
        AccountInfoLib.AccountInfo memory account = init.path.nodes[idx].node.account;
        bytes memory bs = ByteUtilLib.bytes32ToBytes(account.addr);
        address addr = ByteUtilLib.bytesToAddress(bs);
        require(addr == msg.sender);

        bool processingFlag = processingWithdrawal(addr);
        require(!processingFlag);

        HubRootLib.HubRoot memory latestRoot = latestRoot();
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, init.path);
        require(proofFlag);

        require(account.allotment >= init.amount);

        WithdrawalLib.Withdrawal memory with;
        with.addr = addr;
        with.info = init;
        with.isVal = true;
        with.stat = WithdrawalStatusTypeLib.WithdrawalStatusType.INIT;

        balances[0].withdrawalMeta.addrs.push(addr);
        // balances[0].withdrawalMeta.withdrawals[addr] = with;//TODO:change path to bytes
        balances[0].withdrawalMeta.total += init.amount;
    }

    function processingWithdrawal(address addr) private view returns (bool flag) {
        flag = true;
        for(uint i=0;i<balances.length;i++) {
            if(balances[i].withdrawalMeta.withdrawals[addr].isVal) {
                WithdrawalLib.Withdrawal memory with = balances[i].withdrawalMeta.withdrawals[addr];

                if(with.stat == WithdrawalStatusTypeLib.WithdrawalStatusType.CANCEL || with.stat == WithdrawalStatusTypeLib.WithdrawalStatusType.CONFIRMED) {
                    flag = false;
                }

                break;
            }
        }
    }

    function cancelWithdrawal(bytes calldata data) external recovery {
        CancelWithdrawalRequestLib.CancelWithdrawalRequest memory cancel = CancelWithdrawalRequestLib.unmarshal(data);
        uint currentEon = currentEon();
        require(cancel.update.eon >= 0 && cancel.update.eon == currentEon);

        bool participantFlag = ParticipantLib.verifyParticipant(cancel.participant);
        require(participantFlag);

        bool signFlag = UpdateLib.verifySig(cancel.participant, cancel.update);
        require(signFlag);

        HubRootLib.HubRoot memory latestRoot = latestRoot();
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, cancel.merklePath);
        require(proofFlag);

        WithdrawalLib.Withdrawal storage with = balances[0].withdrawalMeta.withdrawals[cancel.participant.addr];
        if(with.isVal) {
            if(with.stat == WithdrawalStatusTypeLib.WithdrawalStatusType.INIT) {
                AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(cancel.merklePath);
                AccountInfoLib.AccountInfo memory account = leaf.account;

                uint tmp = SafeMath.sub(SafeMath.add(account.allotment, cancel.update.receiveAmount), cancel.update.sendAmount);
                if (with.info.amount > tmp) {
                    with.stat = WithdrawalStatusTypeLib.WithdrawalStatusType.CANCEL;
                    balances[0].withdrawalMeta.withdrawals[cancel.participant.addr] = with;
                }
            }
        }
    }

    function openBalanceUpdateChallenge(bytes calldata data) external recovery {
        BalanceUpdateChallengeLib.BalanceUpdateChallenge memory challenge = BalanceUpdateChallengeLib.unmarshal(data);
        require(challenge.proof.hasPath || challenge.proof.hasUp);

        if(challenge.proof.hasPath) {
            uint tmpEon =  challenge.proof.path.eon;
            require(tmpEon > 0);
            require(balances[1].hasRoot);
            require(tmpEon == balances[1].eon);

            HubRootLib.HubRoot memory root = balances[1].root;
            AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(root);
            bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, challenge.proof.path);
            require(proofFlag);
        } else {
            DepositLib.Deposit memory d = all[msg.sender];
            require(!d.hasVal);
        }

        if(challenge.proof.hasUp) {
            UpdateLib.Update memory up = challenge.proof.update;

            uint tmpEon = up.eon;
            require(tmpEon > 0);
            require(tmpEon == balances[1].eon);

            bool signFlag = UpdateLib.verifyHubSig(up, hubPK);
            require(signFlag);
        } else {

        }

        BalanceUpdateChallengeStatusLib.BalanceUpdateChallengeStatus memory cs;
        cs.challenge = challenge;
        cs.status = ChallengeStatusLib.ChallengeStatus.OPEN;

        // balances[0].balanceChallenges[msg.sender] = cs;//TODO:change path to bytes
    }

    function closeBalanceUpdateChallenge(bytes calldata data) external recovery {
        BalanceUpdateProofLib.BalanceUpdateProof memory proof = BalanceUpdateProofLib.unmarshal(data);

        HubRootLib.HubRoot memory root = balances[1].root;
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(root);
        bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, proof.path);
        require(proofFlag);

        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(proof.path);
        AccountInfoLib.AccountInfo memory account = leaf.account;
        bytes memory bs = ByteUtilLib.bytes32ToBytes(account.addr);
        address addr = ByteUtilLib.bytesToAddress(bs);

        BalanceUpdateChallengeStatusLib.BalanceUpdateChallengeStatus storage stat = balances[0].balanceChallenges[addr];
        require(stat.isVal);
        if(stat.status == ChallengeStatusLib.ChallengeStatus.OPEN) {
            uint d = balances[1].depositMeta.deposits[addr];
            if(proof.update.sendAmount == 0 && proof.update.receiveAmount == 0) {
                if(d > 0) {
                    require(d <= account.allotment);
                }
            } else {
                ParticipantLib.Participant memory participant;
                participant.publicKey = stat.challenge.publicKey;
                bool signFlag = UpdateLib.verifySig(participant, proof.update);
                require(signFlag);
                bool signFlag2 = UpdateLib.verifyHubSig(proof.update, hubPK);
                require(signFlag2);
                require(proof.update.version >= stat.challenge.proof.update.version);
            }

            WithdrawalLib.Withdrawal memory w = balances[1].withdrawalMeta.withdrawals[addr];
            uint preAllotment = 0;
            if(stat.challenge.proof.hasPath) {
                AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory tmp = AugmentedMerklePathLib.leafNode(stat.challenge.proof.path);
                preAllotment = tmp.account.allotment;
            }

            uint t1 = SafeMath.add(proof.update.receiveAmount, preAllotment);
            t1 = SafeMath.add(t1, d);
            t1 = SafeMath.sub(t1, w.info.amount);
            uint t2 = proof.update.sendAmount;
            uint allotment = SafeMath.sub(t1, t2);
            require(allotment == leaf.allotment);

            require(UpdateLib.equals(proof.update, account.update));

            stat.status == ChallengeStatusLib.ChallengeStatus.CLOSE;
            balances[0].balanceChallenges[addr] = stat;
        }
    }

    function openTransferDeliveryChallenge(bytes calldata data) external recovery {
        OpenTransferDeliveryChallengeRequestLib.OpenTransferDeliveryChallengeRequest memory open = OpenTransferDeliveryChallengeRequestLib.unmarshal(data);
        require(open.update.eon >= 0);
        require(open.update.eon == balances[1].eon);

        bool signFlag = UpdateLib.verifyHubSig(open.update, hubPK);
        require(signFlag);

        bool verifyFlag = MerkleTreeLib.verifyMembershipProof(open.update.root, open.path);
        require(verifyFlag);

        MerklePathNodeLib.MerklePathNode memory leaf = MerklePathLib.leafNode(open.path);
        require(OffchainTransactionLib.equals(open.tran, leaf.node.tran));

        TransferDeliveryChallengeLib.TransferDeliveryChallenge memory challenge;
        challenge.tran = open.tran;
        challenge.update = open.update;
        challenge.path = open.path;
        challenge.stat = ChallengeStatusLib.ChallengeStatus.OPEN;
        challenge.isVal = true;

        // bytes32 hash = OffchainTransactionLib.hash(open.tran);
        // string memory key = string(ByteUtilLib.bytes32ToBytes(hash));
        // balances[0].transferChallenges[key] = challenge;//TODO:change path to bytes
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external recovery {
        CloseTransferDeliveryChallengeRequestLib.CloseTransferDeliveryChallengeRequest memory close = CloseTransferDeliveryChallengeRequestLib.unmarshal(data);

        HubRootLib.HubRoot memory latestRoot = latestRoot();
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, close.merklePath);
        require(proofFlag);

        MerklePathNodeLib.MerklePathNode memory leaf = MerklePathLib.leafNode(close.path);
        bytes32 hash = OffchainTransactionLib.hash(leaf.node.tran);
        string memory key = string(ByteUtilLib.bytes32ToBytes(hash));
        TransferDeliveryChallengeLib.TransferDeliveryChallenge memory challenge = balances[0].transferChallenges[key];
        require(challenge.isVal);

        if(challenge.stat == ChallengeStatusLib.ChallengeStatus.OPEN) {
            ParticipantLib.Participant memory participant;
            participant.publicKey = close.fromPublicKey;
            bool signFlag = UpdateLib.verifySig(participant, close.update);
            require(signFlag);

            AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf2 = AugmentedMerklePathLib.leafNode(close.merklePath);
            require(UpdateLib.equals(leaf2.account.update, close.update));

            bool verifyFlag = MerkleTreeLib.verifyMembershipProof(close.update.root, close.path);
            require(verifyFlag);

            challenge.stat = ChallengeStatusLib.ChallengeStatus.CLOSE;
            // balances[0].transferChallenges[key] = challenge;//TODO:change path to bytes
        }
    }

    function recoverFunds(bytes calldata data) external recovery {
        AugmentedMerklePathLib.AugmentedMerklePath memory path = AugmentedMerklePathLib.unmarshal(data);

        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory leaf = AugmentedMerklePathLib.leafNode(path);
        require(ByteUtilLib.bytesToAddress(ByteUtilLib.bytes32ToBytes(leaf.account.addr)) == msg.sender);

        require(balances[1].eon == path.eon);

        HubRootLib.HubRoot memory latestRoot = latestRoot();
        AugmentedMerkleTreeNodeLib.AugmentedMerkleTreeNode memory merkle = HubRootLib.hubRoot2AugmentedMerkleTreeNode(latestRoot);
        bool proofFlag = AugmentedMerkleTreeLib.verifyMembershipProof(merkle, path);
        require(proofFlag);

        uint amount = SafeMath.add(leaf.allotment, balances[0].depositMeta.deposits[msg.sender]);
        amount = SafeMath.add(amount, balances[1].depositMeta.deposits[msg.sender]);
        require(amount > 0);

        RecoveryMetaLib.RecoveryMeta memory r = recoverys[msg.sender];
        require(!r.isVal);
        r.isVal = true;
        recoverys[msg.sender] = r;
        msg.sender.transfer(amount);
    }

    function getLatestRoot() external recovery returns (bytes memory) {
        return HubRootLib.marshal(latestRoot());
    }

    function latestRoot() private view returns (HubRootLib.HubRoot memory) {
        return balances[0].root;
    }

    function getCurrentEon() external recovery returns (uint) {
        return 1;
    }

    function currentEon() private view returns (uint) {
        return balances[0].eon;
    }

    function isRecoveryMode() external recovery returns (bool) {
        return recoveryMode;
    }
}