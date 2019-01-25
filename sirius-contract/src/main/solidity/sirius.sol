pragma solidity ^0.5.1;

import "./lib/model.sol";
import "./lib/safe_math.sol";

interface Sirius {
    function commit(bytes calldata data) external returns (bool);
    function initiateWithdrawal(bytes calldata data) external returns (bool);
    function cancelWithdrawal(bytes calldata data) external returns (bool);
    function openBalanceUpdateChallenge(bytes calldata data) external returns (bool);
    function closeBalanceUpdateChallenge(bytes calldata data) external returns (bool);
    function openTransferDeliveryChallenge(bytes calldata data) external returns (bool);
    function closeTransferDeliveryChallenge(bytes calldata data) external returns (bool);
    function recoverFunds(bytes calldata data) external;
    function getLatestRoot() external returns (bytes memory);
    function getCurrentEon() external view returns (uint);
    function isRecoveryMode() external view returns (bool);
    function test() external view returns (bool);
    function testRecovery() external;
    function hubIp(bytes calldata data) external;
    function hubInfo() external view returns (bytes memory);
    function queryWithdrawal(uint eon) external returns (bytes memory);
    function queryBalance(uint eon) external view returns (bytes memory);
    function queryTransfer(uint eon, bytes32 txHash) external view returns (bytes memory);
}

contract SiriusService is Sirius {
    address private owner;
    bytes32 private ownerHash;
    bool private recoveryMode;
    uint private startHeight;
    uint private blocksPerEon;
    bytes private hubPK;
    string ip;

    GlobleLib.Balance[3] balances;
    GlobleLib.DataStore dataStore;

    mapping(address => bool) private recoverys;//used for recovery model

    using SafeMath for uint;
    event SiriusEvent(bytes32 indexed hash, uint indexed num, bytes value);
    event SiriusEvent2(address indexed addr, uint indexed num, bytes value);
    event SiriusEvent3(uint indexed num, bool value);
    event SiriusEvent4(uint indexed num, uint value);

    constructor(bytes memory data) public {
        owner = msg.sender;
        ownerHash = ByteUtilLib.address2hash(msg.sender);
        GlobleLib.Balance memory initBalance = newBalance(0);
        checkBalances(initBalance);

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
        if(!recoveryMode) {
            bool flag = true;
            uint bLen = balances[1].balanceChallenges.length;
            //balance
            for(uint i=0;i<bLen;i++) {
                address addr = balances[1].balanceChallenges[i];
                GlobleLib.BalanceUpdateChallengeAndStatus memory s = dataStore.bucData[balances[1].eon][addr];
                if(s.isVal && s.status != ModelLib.ChallengeStatus.CLOSE) {
                    flag = false;
                    break;
                }
            }


            //transfer
            if(flag) {
                uint tLen = balances[1].transferChallenges.length;
                for(uint i=0;i<tLen;i++) {
                    bytes32 tKey = balances[1].transferChallenges[i];
                    GlobleLib.TransferDeliveryChallengeAndStatus memory t = dataStore.tdcData[balances[1].eon][tKey];
                   if(t.isVal && t.stat != ModelLib.ChallengeStatus.CLOSE) {
                       flag = false;
                       break;
                   }
                }
            }

            if(flag) {
                ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(data, true));
                require(!balances[0].hasRoot, "root exist");
                require(root.eon > 0, "eon == 0");
                require(balances[0].eon == root.eon, "eon err");
                ModelLib.hubRootCommonVerify(root);
                uint tmp = SafeMath.add(balances[1].root.node.allotment, balances[1].depositTotal);
                uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalTotal);
                require(allotmentTmp == root.node.allotment, ByteUtilLib.appendUintToString("allotment error:",allotmentTmp));
                balances[0].root = root;
                balances[0].hasRoot = true;

                //withdrawal
                uint wLen = balances[2].withdrawals.length;
                for(uint i=0;i<wLen;i++) {
                    address payable addr = balances[2].withdrawals[i];
                    GlobleLib.Withdrawal memory w = dataStore.withdrawalData[balances[2].eon][addr];
                    if(w.isVal && w.stat == GlobleLib.WithdrawalStatusType.INIT) {
                        w.stat = GlobleLib.WithdrawalStatusType.CONFIRMED;
                        dataStore.withdrawalData[balances[2].eon][addr] = w;
                        ModelLib.WithdrawalInfo memory wi = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(w.info, true));
                        addr.transfer(wi.amount);
                    }
                }

                return true;
            }
        }

        return false;
    }

    function initiateWithdrawal(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            address payable addr = msg.sender;
            ModelLib.WithdrawalInfo memory init = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(data, true));
            require(init.amount > 0);
            require(init.proof.path.eon > 0);
            require(init.proof.path.leaf.allotment >= init.amount);
            uint preEon = balances[1].eon;
            ModelLib.verifyProof(preEon, addr, owner, init.proof);

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
            return true;
        } else {
            return false;
        }
    }

    function cancelWithdrawal(bytes calldata data) external returns (bool) {
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

            return true;
        }
        return false;
    }

    function openBalanceUpdateChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            ModelLib.BalanceUpdateProof memory open = ModelLib.unmarshalBalanceUpdateProof(RLPDecoder.toRLPItem(data, true));
            require(open.hasPath || open.hasUp, "miss path and update");
            require(balances[0].hasRoot, "balances[0].hasRoot false");

            uint preEon = balances[1].eon;
            if(open.hasPath) {//Special case:eon-1 exist a account, evil owner removed it in this eon, so the account has only path
                require(preEon == open.path.eon, ByteUtilLib.appendUintToString("expect path eon:", preEon));

                ModelLib.HubRoot memory preRoot = balances[1].root;
                bool proofFlag = ModelLib.verifyMembershipProof4AMTreePath(preRoot.node, open.path);
                require(proofFlag, "verify proof fail");
            } else {}

            if(open.hasUp) {//Special case:new account only update
                ModelLib.Update memory up = open.update;
                require(up.upData.eon == preEon, ByteUtilLib.appendUintToString("expect update eon:", preEon));

                bool signFlag = ModelLib.verifySign4Update(up.upData, up.sign, msg.sender);
                require(signFlag, "verify update sign fail");
                bool hubSignFlag = ModelLib.verifySign4Update(up.upData, up.hubSign, owner);
                require(hubSignFlag, "verify hub sign fail");
            } else {}

            GlobleLib.BalanceUpdateChallengeAndStatus memory cs = dataStore.bucData[balances[0].eon][msg.sender];
            cs.challenge = data;
            cs.status = ModelLib.ChallengeStatus.OPEN;
            if(!cs.isVal) {
                balances[0].balanceChallenges.push(msg.sender);
            }
            cs.isVal = true;
            dataStore.bucData[balances[0].eon][msg.sender] = cs;
            return true;
        }else {
            return false;
        }
    }

    function closeBalanceUpdateChallenge(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));
            require(balances[0].hasRoot, "require root");

            uint eon = currentEon();
            ModelLib.verifyProof(eon, close.addr, owner, close.proof);

            ModelLib.HubRoot memory root = balances[0].root;
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(root.node, close.proof);
            require(proofFlag, "verify membership proof fail.");

            GlobleLib.BalanceUpdateChallengeAndStatus storage tmpStat = dataStore.bucData[balances[0].eon][close.addr];
require(tmpStat.isVal, "check challenge status fail");

            ModelLib.BalanceUpdateChallengeStatus memory stat = GlobleLib.change2BalanceUpdateChallengeStatus(tmpStat);

            if(stat.status == ModelLib.ChallengeStatus.OPEN) {
                uint d = dataStore.depositData[balances[1].eon][close.addr];

                uint preAllotment = 0;
                if(stat.proof.hasPath) {
                    preAllotment = stat.proof.path.leaf.allotment;
                }

                uint t1 = SafeMath.add(close.proof.leaf.update.upData.receiveAmount, preAllotment);
                t1 = SafeMath.add(t1, d);
                GlobleLib.Withdrawal memory w = dataStore.withdrawalData[balances[1].eon][close.addr];
                if(w.isVal) {
                    ModelLib.WithdrawalInfo memory info = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(w.info, true));
                    t1 = SafeMath.sub(t1, info.amount);
                }
                uint t2 = close.proof.leaf.update.upData.sendAmount;
                uint allotment = SafeMath.sub(t1, t2);
require(allotment == close.proof.path.leaf.allotment, "check proof allotment fail.");

                tmpStat.status == ModelLib.ChallengeStatus.CLOSE;
                dataStore.bucData[balances[0].eon][close.addr] = tmpStat;
                emit SiriusEvent2(close.addr, 2, tmpStat.challenge);
            }
            return true;
        } else {
            return false;
        }
    }

    function openTransferDeliveryChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            ModelLib.TransferDeliveryChallenge memory open = ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
            require(balances[0].hasRoot, "balances[0].hasRoot false");
            require(open.update.upData.eon == balances[1].eon);
            require(open.tran.offData.eon == balances[1].eon);

            bool signFlag = ModelLib.verifySign4Update(open.update.upData, open.update.sign, msg.sender);
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
            return true;
        } else {
            return false;
        }
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
            require(balances[0].hasRoot, "balances[0].hasRoot false");

            address addr = close.fromAddr;
            ModelLib.verifyProof(balances[0].eon, addr, owner, close.proof);
            bytes32 key = close.txHash;

            ModelLib.HubRoot memory latestRoot = balances[0].root;
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(latestRoot.node, close.proof);
            require(proofFlag);

            GlobleLib.TransferDeliveryChallengeAndStatus memory challenge = dataStore.tdcData[balances[0].eon][key];
            require(challenge.isVal);

            if(challenge.stat == ModelLib.ChallengeStatus.OPEN) {
                bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(close.proof.leaf.update.upData.root, close.txPath, close.txHash);
                require(verifyFlag);

                challenge.stat = ModelLib.ChallengeStatus.CLOSE;
                dataStore.tdcData[balances[0].eon][key] = challenge;
                emit SiriusEvent(key, 3, challenge.challenge);
            }
            return true;
        } else {
            return false;
        }
    }

    function recoverFunds(bytes calldata data) external {
        require(recoveryMode);

        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data, true));

        bytes32 key = ByteUtilLib.address2hash(msg.sender);
        require(proof.leaf.addressHash == key);

        uint preEon = balances[1].eon;
        require(preEon == proof.leaf.update.upData.eon);

        ModelLib.HubRoot memory preRoot = getPreRoot();
        bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(preRoot.node, proof);
        require(proofFlag);

        uint amount = SafeMath.add(proof.path.leaf.allotment, dataStore.depositData[balances[0].eon][msg.sender]);
        amount = SafeMath.add(amount, dataStore.depositData[balances[1].eon][msg.sender]);
        require(amount > 0);

        bool flag = recoverys[msg.sender];
        require(!flag);
        recoverys[msg.sender] = true;
        msg.sender.transfer(amount);
    }

    function getLatestRoot() external returns (bytes memory) {
        ModelLib.HubRoot memory root = latestRoot();
        bytes memory tmp = ModelLib.marshalHubRoot(root);
        return tmp;
    }

    function getCurrentEon() external view returns (uint) {
        return currentEon();
    }

    function isRecoveryMode() external view returns (bool) {
        return recoveryMode;
    }

    function test() external view returns (bool) {
        return true;
    }

    function hubInfo() external view returns (bytes memory) {
        ModelLib.ContractHubInfo memory chi;
        chi.startBlockNum = startHeight;
        chi.hubAddress = ip;
        chi.blocksPerEon = blocksPerEon;
        chi.latestEon = currentEon();
        return ModelLib.marshalContractHubInfo(chi);
    }

    function testRecovery() external {
        require(msg.sender == owner);
        recoveryMode = true;
    }

    function queryWithdrawal(uint eon) external returns (bytes memory) {
        GlobleLib.Withdrawal memory tmp;
        ModelLib.ContractReturn memory cr;
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = dataStore.withdrawalData[balances[i].eon][msg.sender];
                break;
            }
        }
        if(tmp.isVal) {
            cr.hasVal = true;
            cr.payload = GlobleLib.marshalWithdrawal(tmp);
        }

        return ModelLib.marshalContractReturn(cr);
    }

    function queryBalance(uint eon) external view returns (bytes memory) {
        GlobleLib.BalanceUpdateChallengeAndStatus memory tmp;
        ModelLib.ContractReturn memory cr;
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = dataStore.bucData[balances[i].eon][msg.sender];
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

    /** private methods **/

    function newBalance(uint newEon) private pure returns(GlobleLib.Balance memory latest) {
        ModelLib.HubRoot memory root;
        address payable[] memory withdrawals;
        address[] memory balanceChallenges;
        bytes32[] memory transferChallenges;
        return GlobleLib.Balance(newEon, false, root, 0, 0, withdrawals, balanceChallenges, transferChallenges);
    }

    function checkBalances(GlobleLib.Balance memory latest) private {
        for (uint i = balances.length; i > 0; i--) {
            uint tmp = i - 1;
            if (tmp == 0) {
                balances[0] = latest;
            } else {
                balances[tmp] = balances[tmp - 1];
            }
        }
    }

    function withdrawalProcessing(address addr) private view returns (bool flag) {
        flag = false;

        for(uint i=0;i<balances.length;i++) {
            GlobleLib.Withdrawal memory with =dataStore.withdrawalData[balances[i].eon][addr];

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
        }

        if (newEon == addEon && !recoveryMode) {// change eon
            GlobleLib.Balance memory latest = newBalance(newEon);
            checkBalances(latest);
        } else {}
    }
}
