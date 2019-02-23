pragma solidity ^0.5.1;

import "./lib/model.sol";
import "./lib/safe_math.sol";

interface Sirius {
    function commit(bytes calldata data) external returns (bool);
    function initiateWithdrawal(bytes calldata data) external returns (bool);
    function cancelWithdrawal(bytes calldata data) external returns (bool);
    function openBalanceUpdateChallenge(bytes calldata data) external returns (bool);
    function closeBalanceUpdateChallenge(address addr, bytes calldata data) external returns (bool);
    function openTransferDeliveryChallenge(bytes calldata data) external returns (bool);
    function closeTransferDeliveryChallenge(bytes calldata data) external returns (bool);
    function recoverFunds(bytes calldata data) external;
    function queryLatestRoot() external view returns (bytes memory);
    function queryCurrentEon() external view returns (uint);
    function queryRecoveryMode() external view returns (bool);
    function hubIp(bytes calldata data) external;
    function queryHubInfo() external view returns (bytes memory);
    function queryWithdrawal() external view returns (bytes memory);
    function queryBalance(uint eon) external returns (bytes memory);
    function queryTransfer(uint eon, bytes32 txHash) external returns (bytes memory);
}

contract SiriusService is Sirius {
    address private owner;
    bool private recoveryMode;
    uint private startHeight;
    uint private blocksPerEon;
    string ip;
    Challenge challengeContract;

    GlobleLib.Balance[3] balances;
    GlobleLib.DataStore dataStore;

    mapping(address => bool) private recoverys;//used for recovery model

    using SafeMath for uint;
    event SiriusEvent(bytes32 indexed hash, uint indexed num, bytes value);
    event SiriusEvent2(address indexed addr, uint indexed num, bytes value);
    event SiriusEvent3(uint indexed num, bool value);

    constructor(address firstAddress, bytes memory data) public {
        owner = msg.sender;
        changeBalance(0);

        ModelLib.ContractConstructArgs memory args = ModelLib.unmarshalContractConstructArgs(RLPDecoder.toRLPItem(data, true));
        require(args.blocks >= 4);
        blocksPerEon = args.blocks;

        ModelLib.HubRoot memory root = args.hubRoot;
        require(root.eon == 0);
        require(root.node.allotment == 0);
        ModelLib.hubRootCommonVerify(root);

        balances[0].root = root;
        balances[0].hasRoot = true;
        startHeight = block.number;
        recoveryMode = false;

        challengeContract = Challenge(firstAddress);
        challengeContract.mainContract();
        address addr = challengeContract.queryOwner();
        require(addr == owner);
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "not owner");
        doRecovery();
        _;
    }

    modifier recovery() {
        doRecovery();
        _;
    }

    /** public methods **/

    function hubIp(bytes calldata data) external onlyOwner {
        ip = string(data);
    }

    function () external payable {
        deposit();
    }

    function deposit() private {
        doRecovery();
        require(msg.value > 0);
        if(!recoveryMode) {
            balances[0].depositTotal = SafeMath.add(balances[0].depositTotal, msg.value);
            dataStore.depositData[balances[0].eon][msg.sender] = SafeMath.add(dataStore.depositData[balances[0].eon][msg.sender], msg.value);
        } else {
            msg.sender.transfer(msg.value);
        }
    }

    function commit(bytes calldata data) external onlyOwner returns (bool) {
        uint currentEon = balances[0].eon;
        bool succ = challengeContract.changeBalance(currentEon);
        if(!succ) {
            recoveryMode = true;
        }

        bool returnFlag = false;
        if(!recoveryMode) {
            //balance and transfer
            bool flag = challengeContract.hasChallenge();
            require(flag);

            if(flag) {
                ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(data, true));
                require(!balances[0].hasRoot, "root exist");
                require(root.eon > 0, "eon == 0");
                require(currentEon == root.eon, "eon err");
                ModelLib.hubRootCommonVerify(root);
                uint tmp = SafeMath.add(balances[1].root.node.allotment, balances[1].depositTotal);
                uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalTotal);
                require(allotmentTmp == root.node.allotment, ByteUtilLib.appendUintToString("allotment error:",allotmentTmp));
                balances[0].root = root;
                balances[0].hasRoot = true;

                //withdrawal
                uint wLen = balances[1].withdrawals.length;
                for(uint i=0;i<wLen;i++) {
                    address payable addr = balances[1].withdrawals[i];
                    GlobleLib.Withdrawal memory w = dataStore.withdrawalData[balances[1].eon][addr];
                    if(w.isVal && w.stat == GlobleLib.WithdrawalStatusType.INIT) {
                        w.stat = GlobleLib.WithdrawalStatusType.CONFIRMED;
                        dataStore.withdrawalData[balances[1].eon][addr] = w;
                        ModelLib.WithdrawalInfo memory wi = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(w.info, true));
                        addr.transfer(wi.amount);
                        emit SiriusEvent2(addr, 3, ByteUtilLib.uint2byte(wi.amount));
                    }
                }
            }

            returnFlag = flag;
        }

        emit SiriusEvent3(2, returnFlag);
        return returnFlag;
    }

    function initiateWithdrawal(bytes calldata data) external recovery returns (bool) {
        bool returnFlag = false;
        if(!recoveryMode) {
            address payable addr = msg.sender;
            ModelLib.WithdrawalInfo memory init = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(data, true));
            require(init.amount > 0);
            require(init.proof.path.eon > 0);
            require(init.proof.path.leaf.allotment >= init.amount);
            uint preEon = balances[1].eon;
            ModelLib.verifyProof(preEon, addr, owner, init.proof, true);

            bool processingFlag = withdrawalProcessing(addr);
            require(!processingFlag);

            ModelLib.HubRoot memory preRoot = getPreRoot();
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(preRoot.node, init.proof);
            require(proofFlag);

            GlobleLib.Withdrawal memory with;
            with.info = data;
            with.isVal = true;
            with.stat = GlobleLib.WithdrawalStatusType.INIT;

            balances[0].withdrawals.push(addr);
            dataStore.withdrawalData[balances[0].eon][addr] = with;
            balances[0].withdrawalTotal = SafeMath.add(balances[0].withdrawalTotal, init.amount);
            returnFlag = true;
        }

        emit SiriusEvent3(3, returnFlag);
        return returnFlag;
    }

    function cancelWithdrawal(bytes calldata data) external onlyOwner returns (bool) {
        bool returnFlag = false;
        if(!recoveryMode) {
            ModelLib.CancelWithdrawal memory cancel = ModelLib.unmarshalCancelWithdrawal(RLPDecoder.toRLPItem(data, true));
            require((cancel.update.upData.eon > cancel.proof.leaf.update.upData.eon || (cancel.update.upData.eon == cancel.proof.leaf.update.upData.eon && cancel.update.upData.version > cancel.proof.leaf.update.upData.version)));
            bytes32 key = ByteUtilLib.address2hash(cancel.addr);
            require(key == cancel.proof.leaf.addressHash);

            //verify proof
            uint hubRootEon = SafeMath.add(cancel.proof.leaf.update.upData.eon, 1);
            ModelLib.HubRoot memory tmpRoot = queryHubRootByEon(hubRootEon);
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(tmpRoot.node, cancel.proof);
            require(proofFlag);

            //verify Update
            bool signFlag = ModelLib.verifySign4Update(cancel.update.upData, cancel.update.sign, cancel.addr);
            require(signFlag);
            bool hubSignFlag = ModelLib.verifySign4Update(cancel.update.upData, cancel.update.hubSign, owner);
            require(hubSignFlag);

            for(uint i=0;i<balances.length;i++) {
                GlobleLib.Withdrawal storage with = dataStore.withdrawalData[balances[i].eon][cancel.addr];

                if(with.isVal) {
                    if(with.stat != GlobleLib.WithdrawalStatusType.CANCEL && with.stat != GlobleLib.WithdrawalStatusType.CONFIRMED) {
                        if(with.stat == GlobleLib.WithdrawalStatusType.INIT) {
                            ModelLib.WithdrawalInfo memory tmpInfo = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(with.info, true));
                            uint tmp = SafeMath.sub(SafeMath.add(cancel.proof.path.leaf.allotment, cancel.update.upData.receiveAmount), cancel.update.upData.sendAmount);
                            if (tmpInfo.amount > tmp) {
                                with.stat = GlobleLib.WithdrawalStatusType.CANCEL;
                                dataStore.withdrawalData[balances[i].eon][cancel.addr] = with;
                                balances[i].withdrawalTotal = SafeMath.sub(balances[i].withdrawalTotal, tmpInfo.amount);
                                emit SiriusEvent2(cancel.addr, 1, ByteUtilLib.uint2byte(tmpInfo.amount));
                            }
                        }
                    }

                    break;
                }
            }

            returnFlag = true;
        }

        emit SiriusEvent3(4, returnFlag);
        return returnFlag;
    }

    function openBalanceUpdateChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            require(balances[0].hasRoot, "balances[0].hasRoot false");
            ModelLib.HubRoot memory preRoot = balances[1].root;
            bytes memory preHr = ModelLib.marshalHubRoot(preRoot);
            return challengeContract.openBalanceUpdateChallenge(data, preHr);
        }
        return false;
    }

    function closeBalanceUpdateChallenge(address addr, bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            require(balances[0].hasRoot, "balances[0].hasRoot false");
            ModelLib.HubRoot memory root = balances[0].root;
            bytes memory hr = ModelLib.marshalHubRoot(root);
            uint depositAmount = dataStore.depositData[balances[1].eon][addr];

            uint withdrawalAmount = 0;
            GlobleLib.Withdrawal memory w = dataStore.withdrawalData[balances[1].eon][addr];
            if(w.isVal) {
                ModelLib.WithdrawalInfo memory info = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(w.info, true));
                withdrawalAmount = info.amount;
            }

            return challengeContract.closeBalanceUpdateChallenge(addr, data, hr, withdrawalAmount, depositAmount);
        }
        return false;
    }

    function openTransferDeliveryChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            return challengeContract.openTransferDeliveryChallenge(data);
        }
        return false;
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            ModelLib.HubRoot memory latestRoot = balances[0].root;
            bytes memory latestHr = ModelLib.marshalHubRoot(latestRoot);
            return challengeContract.closeTransferDeliveryChallenge(data, latestHr);
        }
        return false;
    }

    function recoverFunds(bytes calldata data) external {
        require(recoveryMode);

        bool flag = recoverys[msg.sender];
        require(!flag);

        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data, true));

        bytes32 key = ByteUtilLib.address2hash(msg.sender);
        require(proof.leaf.addressHash == key);

        uint preEon;
        ModelLib.HubRoot memory preRoot;
        if(balances[0].hasRoot) {
            preEon = balances[1].eon;
            preRoot = balances[0].root;
        } else if(!balances[0].hasRoot) {
            preEon = balances[2].eon;
            preRoot = balances[1].root;
        }
        require(preEon == proof.leaf.update.upData.eon);

        bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(preRoot.node, proof);
        require(proofFlag);

        uint amount = SafeMath.add(proof.path.leaf.allotment, dataStore.depositData[balances[0].eon][msg.sender]);
        amount = SafeMath.add(amount, dataStore.depositData[balances[1].eon][msg.sender]);
        require(amount > 0);

        recoverys[msg.sender] = true;
        msg.sender.transfer(amount);
    }

    function queryLatestRoot() external view returns (bytes memory) {
        ModelLib.ContractReturn memory cr;
        if(balances[0].hasRoot || balances[1].hasRoot) {
            ModelLib.HubRoot memory root = latestRoot();
            bytes memory tmp = ModelLib.marshalHubRoot(root);
            cr.hasVal = true;
            cr.payload = tmp;
        }

        return ModelLib.marshalContractReturn(cr);
    }

    function queryCurrentEon() external view returns (uint) {
        return currentEon();
    }

    function queryRecoveryMode() external view returns (bool) {
        return recoveryMode;
    }

    function queryHubInfo() external view returns (bytes memory) {
        ModelLib.ContractReturn memory cr;
        cr.hasVal = true;
        ModelLib.ContractHubInfo memory chi;
        chi.startBlockNum = startHeight;
        chi.hubAddress = ip;
        chi.blocksPerEon = blocksPerEon;
        chi.latestEon = currentEon();
        cr.payload = ModelLib.marshalContractHubInfo(chi);

        return ModelLib.marshalContractReturn(cr);
    }

    function queryWithdrawal() external view returns (bytes memory) {
        GlobleLib.Withdrawal memory tmp;
        ModelLib.ContractReturn memory cr;
        for (uint i=0; i < balances.length; i++) {
            tmp = dataStore.withdrawalData[balances[i].eon][msg.sender];

            if(tmp.isVal) {
                cr.hasVal = true;
                cr.payload = GlobleLib.marshalWithdrawal(tmp);
                break;
            }
        }

        return ModelLib.marshalContractReturn(cr);
    }

    function queryBalance(uint eon) external returns (bytes memory) {
        return challengeContract.queryBalance(eon);
    }

    function queryTransfer(uint eon, bytes32 txHash) external returns (bytes memory) {
        return challengeContract.queryTransfer(eon, txHash);
    }

    /** private methods **/

    function changeBalance(uint newEon) private returns (bool flag) {
        for (uint i = balances.length; i > 0; i--) {
            uint tmp = i - 1;
            if (tmp == 0) {
                ModelLib.HubRoot memory root;
                address payable[] memory withdrawals;
                balances[0] = GlobleLib.Balance(newEon, false, root, 0, 0, withdrawals);
            } else {
                balances[tmp] = balances[tmp - 1];
            }
        }
        return true;
    }

    function withdrawalProcessing(address addr) private view returns (bool flag) {
        flag = false;

        for(uint i=0;i<balances.length;i++) {
            GlobleLib.Withdrawal memory with = dataStore.withdrawalData[balances[i].eon][addr];

            if(with.isVal && with.stat != GlobleLib.WithdrawalStatusType.CANCEL && with.stat != GlobleLib.WithdrawalStatusType.CONFIRMED) {
                flag = true;
            }

            if(with.isVal) {
                break;
            }
        }

        return flag;
    }

    function latestRoot() private view returns (ModelLib.HubRoot memory) {
        if(balances[0].hasRoot) {
            return balances[0].root;
        } else {
            return balances[1].root;
        }
    }

    function getPreRoot() private view returns (ModelLib.HubRoot memory) {
        ModelLib.HubRoot memory preRoot = balances[1].root;
        if(balances[0].eon == 0) {
            preRoot = latestRoot();
        }

        return preRoot;
    }

    function queryHubRootByEon(uint eon) private view returns (ModelLib.HubRoot memory) {
        if(balances[0].eon == eon) {
            return balances[0].root;
        } else if(balances[1].eon == eon) {
            return balances[1].root;
        } else if(balances[2].eon == eon) {
            return balances[2].root;
        } else {
            revert();
        }
    }

    function currentEon() private view returns (uint) {
        return balances[0].eon;
    }

    function doRecovery() private {
        require(!recoveryMode, "recovery mode err");

        //current eon
        uint tmp = SafeMath.sub(block.number, startHeight);
        uint newEon = SafeMath.div(tmp, blocksPerEon);
        uint latestEon = currentEon();
        uint addEon = SafeMath.add(balances[0].eon, 1);

        //recovery
        uint tmp2 = SafeMath.add(SafeMath.mul(blocksPerEon, latestEon), SafeMath.div(blocksPerEon, 4));
        uint tmp3 = SafeMath.add(tmp2, blocksPerEon);
        if ((newEon > addEon) || (newEon == addEon && tmp > tmp3 && balances[0].hasRoot) || (newEon == latestEon && tmp > tmp2 && !balances[0].hasRoot)) {
            recoveryMode = true;
            emit SiriusEvent3(1, recoveryMode);
        }

        if (newEon == addEon && !recoveryMode) {// change eon
            changeBalance(newEon);
        } else {}
    }
}

interface Challenge {
    function hasChallenge() external view returns (bool);
    function changeBalance(uint eon) external returns (bool);
    function openBalanceUpdateChallenge(bytes calldata data, bytes calldata preHr) external returns (bool);
    function closeBalanceUpdateChallenge(address addr, bytes calldata data, bytes calldata hr, uint withdrawalAmount, uint depositAmount) external returns (bool);
    function openTransferDeliveryChallenge(bytes calldata data) external returns (bool);
    function closeTransferDeliveryChallenge(bytes calldata data, bytes calldata hr) external returns (bool);
    function queryBalance(uint eon) external view returns (bytes memory);
    function queryTransfer(uint eon, bytes32 txHash) external view returns (bytes memory);
    function queryOwner() external view returns (address);
    function mainContract() external;
}

contract ChallengeService is Challenge {

    GlobleLib.ChallengeBalance[3] balances;
    GlobleLib.ChallengeDataStore dataStore;
    address private owner;
    address private mainContractAddress;
    bool initMain = false;

    constructor() public {
        owner = msg.sender;
        address[] memory balanceChallenges;
        bytes32[] memory transferChallenges;
        balances[0] = GlobleLib.ChallengeBalance(0, balanceChallenges, transferChallenges);
    }

    using SafeMath for uint;
    event ChallengeEvent(bytes32 indexed hash, uint indexed num, bytes value);
    event ChallengeEvent2(address indexed addr, uint indexed num, bytes value);
    event ChallengeEvent3(uint indexed num, bool value);

    modifier checkMain() {
        require(msg.sender == mainContractAddress, "not main");
        _;
    }

    modifier onlyOwner() {
        require(msg.sender == mainContractAddress, "not main");
        require(tx.origin == owner, "not owner");
        _;
    }

    function mainContract() external {
        require(tx.origin == owner, "not owner");
        require(!initMain);
        mainContractAddress = msg.sender;
        initMain = true;
    }

    function queryOwner() external onlyOwner view returns (address) {
        return owner;
    }

    function hasChallenge() external view onlyOwner returns (bool) {
        bool flag = true;
        uint bLen = balances[1].balanceChallenges.length;
        //balance
        if(bLen > 0) {
            for(uint i=0;i<bLen;i++) {
                address addr = balances[1].balanceChallenges[i];
                GlobleLib.BalanceUpdateChallengeAndStatus memory s = dataStore.bucData[balances[1].eon][addr];
                if(s.isVal && s.status != ModelLib.ChallengeStatus.CLOSE) {
                    flag = false;
                    break;
                }
            }
        }

        //transfer
        if(flag) {
            uint tLen = balances[1].transferChallenges.length;
            if(tLen > 0) {
                for(uint i=0;i<tLen;i++) {
                    bytes32 tKey = balances[1].transferChallenges[i];
                    GlobleLib.TransferDeliveryChallengeAndStatus memory t = dataStore.tdcData[balances[1].eon][tKey];
                    if(t.isVal && t.stat != ModelLib.ChallengeStatus.CLOSE) {
                        flag = false;
                        break;
                    }
                }
            }
        }

        return flag;
    }

    function changeBalance(uint newEon) public onlyOwner returns (bool) {
        require(newEon > 0);
        if(newEon == SafeMath.add(balances[0].eon, 1)) {
            for (uint i = balances.length; i > 0; i--) {
                uint tmp = i - 1;
                if (tmp == 0) {
                    address[] memory balanceChallenges;
                    bytes32[] memory transferChallenges;
                    balances[0] = GlobleLib.ChallengeBalance(newEon, balanceChallenges, transferChallenges);
                } else {
                    balances[tmp] = balances[tmp - 1];
                }
            }
        } else {
            return false;
        }

        return true;
    }

    function openBalanceUpdateChallenge(bytes calldata data, bytes calldata preHr) external checkMain returns (bool) {
        ModelLib.BalanceUpdateProof memory open = ModelLib.unmarshalBalanceUpdateProof(RLPDecoder.toRLPItem(data, true));
        require(open.hasPath || open.hasUp, "miss path and update");

        uint preEon = balances[1].eon;
        if(open.hasPath) {//Special case:eon-1 exist a account, evil owner removed it in this eon, so the account has only path
            require(preEon == open.path.eon, ByteUtilLib.appendUintToString("expect path eon:", preEon));

            ModelLib.HubRoot memory preRoot = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(preHr, true));
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(preRoot.node, open.path);
            require(proofFlag, "verify proof fail");
        } else {}

        if(open.hasUp) {//Special case:new account only update
            ModelLib.Update memory up = open.update;
            require(up.upData.eon == preEon, ByteUtilLib.appendUintToString("expect update eon:", preEon));

            bool signFlag = ModelLib.verifySign4Update(up.upData, up.sign, tx.origin);
            require(signFlag, "verify update sign fail");
            bool hubSignFlag = ModelLib.verifySign4Update(up.upData, up.hubSign, owner);
            require(hubSignFlag, "verify hub sign fail");
        } else {}

        GlobleLib.BalanceUpdateChallengeAndStatus memory cs = dataStore.bucData[balances[0].eon][tx.origin];
        cs.challenge = data;
        cs.status = ModelLib.ChallengeStatus.OPEN;
        if(!cs.isVal) {
            balances[0].balanceChallenges.push(tx.origin);
        }
        cs.isVal = true;
        dataStore.bucData[balances[0].eon][tx.origin] = cs;

        emit ChallengeEvent3(5, true);
        return true;
    }

    function closeBalanceUpdateChallenge(address addr, bytes calldata data, bytes calldata hr, uint withdrawalAmount, uint depositAmount) external onlyOwner returns (bool) {
        ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));
        require(addr == close.addr);
        ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(hr, true));

        bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(root.node, close.proof);
        require(proofFlag, "verify membership proof fail.");

        GlobleLib.BalanceUpdateChallengeAndStatus storage tmpStat = dataStore.bucData[balances[0].eon][close.addr];
        require(tmpStat.isVal, "check challenge status fail");

        ModelLib.BalanceUpdateChallengeStatus memory stat = GlobleLib.change2BalanceUpdateChallengeStatus(tmpStat);

        if(stat.status == ModelLib.ChallengeStatus.OPEN) {
            if(!stat.proof.hasUp) {
                ModelLib.verifyProof(balances[0].eon, close.addr, owner, close.proof, false);
            } else {
                require(close.proof.leaf.update.upData.version >= stat.proof.update.upData.version);
                ModelLib.verifyProof(balances[0].eon, close.addr, owner, close.proof, true);
            }

            if(stat.proof.hasPath) {
                uint preAllotment = stat.proof.path.leaf.allotment;

                uint t1 = SafeMath.add(close.proof.leaf.update.upData.receiveAmount, preAllotment);
                t1 = SafeMath.add(t1, depositAmount);
                t1 = SafeMath.sub(t1, withdrawalAmount);
                uint t2 = close.proof.leaf.update.upData.sendAmount;
                uint allotment = SafeMath.sub(t1, t2);
                require(allotment == close.proof.path.leaf.allotment, "check proof allotment fail.");
            }

            tmpStat.status = ModelLib.ChallengeStatus.CLOSE;
            dataStore.bucData[balances[0].eon][close.addr] = tmpStat;
            emit ChallengeEvent2(close.addr, 2, tmpStat.challenge);
        }

        emit ChallengeEvent3(6, true);
        return true;
    }

    function openTransferDeliveryChallenge(bytes calldata data) external checkMain returns (bool) {
        ModelLib.TransferDeliveryChallenge memory open = ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
        require(open.update.upData.eon == balances[1].eon);
        require(open.tran.offData.eon == balances[1].eon);

        bool signFlag = ModelLib.verifySign4Update(open.update.upData, open.update.sign, tx.origin);
        require(signFlag, "verify update sign fail");
        bool hubSignFlag = ModelLib.verifySign4Update(open.update.upData, open.update.hubSign, owner);
        require(hubSignFlag, "verify hub sign fail");

        bytes32 hash = ModelLib.hash4OffchainTransaction(open.tran);
        bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(open.update.upData.root, open.path, hash);
        require(verifyFlag);

        GlobleLib.TransferDeliveryChallengeAndStatus memory challenge = dataStore.tdcData[balances[0].eon][hash];
        challenge.challenge = data;
        challenge.stat = ModelLib.ChallengeStatus.OPEN;
        if(!challenge.isVal) {
            balances[0].transferChallenges.push(hash);
        }
        challenge.isVal = true;
        dataStore.tdcData[balances[0].eon][hash] = challenge;

        emit ChallengeEvent3(7, true);
        return true;
    }

    function closeTransferDeliveryChallenge(bytes calldata data, bytes calldata hr) external onlyOwner returns (bool) {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
        ModelLib.HubRoot memory latestRoot = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(hr, true));

        address addr = close.fromAddr;
        ModelLib.verifyProof(balances[0].eon, addr, owner, close.proof, true);
        bytes32 key = close.txHash;

        bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(latestRoot.node, close.proof);
        require(proofFlag);

        GlobleLib.TransferDeliveryChallengeAndStatus memory challenge = dataStore.tdcData[balances[0].eon][key];
        require(challenge.isVal);

        if(challenge.stat == ModelLib.ChallengeStatus.OPEN) {
            bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(close.proof.leaf.update.upData.root, close.txPath, close.txHash);
            require(verifyFlag);

            challenge.stat = ModelLib.ChallengeStatus.CLOSE;
            dataStore.tdcData[balances[0].eon][key] = challenge;
            emit ChallengeEvent(key, 3, challenge.challenge);
        }

        emit ChallengeEvent3(8, true);
        return true;
    }

    function queryBalance(uint eon) external view returns (bytes memory) {
        GlobleLib.BalanceUpdateChallengeAndStatus memory tmp;
        ModelLib.ContractReturn memory cr;
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = dataStore.bucData[balances[i].eon][tx.origin];
                break;
            }
        }
        if(tmp.isVal) {
            ModelLib.BalanceUpdateChallengeStatus memory cs = GlobleLib.change2BalanceUpdateChallengeStatus(tmp);
            cr.hasVal = true;
            cr.payload = ModelLib.marshalBalanceUpdateChallengeStatus(cs);
        }

        return ModelLib.marshalContractReturn(cr);
    }

    function queryTransfer(uint eon, bytes32 txHash) external view returns (bytes memory) {
        GlobleLib.TransferDeliveryChallengeAndStatus memory tmp;
        ModelLib.ContractReturn memory cr;
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = dataStore.tdcData[balances[i].eon][txHash];
                break;
            }
        }
        if(tmp.isVal) {
            cr.hasVal = true;
            cr.payload = GlobleLib.marshalTransferDeliveryChallengeAndStatus(tmp);
        }

        return ModelLib.marshalContractReturn(cr);
    }
}
