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
    bool private recoveryMode = false;
    uint private startHeight;
    uint private blocksPerEon;
    bytes private hubPK;
    string ip;

    GlobleLib.Balance[3] balances;

    mapping(address => GlobleLib.Deposit) private all;//used when evil owner remove a account, the removed account can also withdrawal
    mapping(address => bool) private recoverys;//used for recovery model

    using SafeMath for uint;
    event SiriusEvent(bytes32 indexed hash, uint indexed num, bytes value);
    event SiriusEvent2(uint indexed num, address value);

    constructor(bytes memory data) public {
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

        owner = msg.sender;
        ownerHash = ByteUtilLib.address2hash(msg.sender);
        startHeight = block.number;
    }

    modifier onlyOwner() {
        require(msg.sender == owner);
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
            GlobleLib.deposit(balances[0].depositMeta, msg.sender, msg.value);

            GlobleLib.Deposit memory d = all[msg.sender];
            d.amount = SafeMath.add(d.amount, msg.value);
            d.hasVal = true;
            all[msg.sender] = d;
        } else {
            msg.sender.transfer(msg.value);
        }
    }

    function commit(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            bool flag = true;
            uint bLen = balances[1].bucMeta.balanceChallengeKeys.length;
            //balance
            for(uint i=0;i<bLen;i++) {
                bytes32 bKey = balances[1].bucMeta.balanceChallengeKeys[i];
                GlobleLib.BalanceUpdateChallengeAndStatus memory s = balances[1].bucMeta.balanceChallenges[bKey];
                if(s.isVal && s.status != ModelLib.ChallengeStatus.CLOSE) {
                    flag = false;
                    break;
                }
            }

            //transfer
            if(flag) {
                uint tLen = balances[1].tdcMeta.transferChallengeKeys.length;
                for(uint i=0;i<tLen;i++) {
                    bytes32 tKey = balances[1].tdcMeta.transferChallengeKeys[i];
                    GlobleLib.TransferDeliveryChallengeAndStatus memory t = balances[1].tdcMeta.transferChallenges[tKey];
                   if(t.isVal && t.stat != ModelLib.ChallengeStatus.CLOSE) {
                       flag = false;
                       break;
                   }
                }
            }

            if(flag) {
                ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(data, true));
                require(!balances[0].hasRoot);
                require(root.eon > 0);
                require(balances[0].eon == root.eon);
                ModelLib.hubRootCommonVerify(root);
                uint tmp = SafeMath.add(balances[1].root.node.allotment, balances[1].depositMeta.total);
                uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalMeta.total);
                require(allotmentTmp == root.node.allotment);
                balances[0].root = root;
                balances[0].hasRoot = true;

                //withdrawal
                uint wLen = balances[2].withdrawalMeta.addrs.length;
                for(uint i=0;i<wLen;i++) {
                    address payable addr = balances[2].withdrawalMeta.addrs[i];
                    bytes32 wKey = ByteUtilLib.address2hash(addr);
                    GlobleLib.Withdrawal memory w = balances[2].withdrawalMeta.withdrawals[wKey];
                   if(w.isVal && w.stat == GlobleLib.WithdrawalStatusType.INIT) {
                       w.stat = GlobleLib.WithdrawalStatusType.CONFIRMED;
                       balances[2].withdrawalMeta.withdrawals[wKey] = w;
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
            require(init.proof.path.leaf.allotment >= init.amount);//TODO  proof decode bug, allotment is wrong
            uint preEon = balances[1].eon;
            bytes32 key = ByteUtilLib.address2hash(addr);
            ModelLib.verifyProof(preEon, addr, owner, init.proof);

            bool processingFlag = withdrawalProcessing(key);
            require(!processingFlag);

            ModelLib.HubRoot memory preRoot = getPreRoot();
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(preRoot.node, init.proof);
            require(proofFlag);

            GlobleLib.Withdrawal memory with;
            with.info = data;
            with.isVal = true;
            with.stat = GlobleLib.WithdrawalStatusType.INIT;

            balances[0].withdrawalMeta.addrs.push(addr);
            balances[0].withdrawalMeta.withdrawals[key] = with;
            balances[0].withdrawalMeta.total += init.amount;
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
                GlobleLib.Withdrawal memory tmpWith = balances[i].withdrawalMeta.withdrawals[key];

                if(tmpWith.isVal && tmpWith.stat != GlobleLib.WithdrawalStatusType.CANCEL && tmpWith.stat != GlobleLib.WithdrawalStatusType.CONFIRMED) {
                    GlobleLib.Withdrawal storage with = balances[i].withdrawalMeta.withdrawals[key];
                    if(with.stat == GlobleLib.WithdrawalStatusType.INIT) {
                        ModelLib.WithdrawalInfo memory tmpInfo = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(with.info, true));
                        uint tmp = SafeMath.sub(SafeMath.add(cancel.proof.path.leaf.allotment, cancel.update.upData.receiveAmount), cancel.update.upData.sendAmount);
                        if (tmpInfo.amount > tmp) {
                            with.stat = GlobleLib.WithdrawalStatusType.CANCEL;
                        }
                    }
                }

                if(tmpWith.isVal) {
                    break;
                }
            }

            return true;
        }
        return false;
    }

    function openBalanceUpdateChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            ModelLib.BalanceUpdateChallenge memory challenge = ModelLib.unmarshalBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));
            require(challenge.proof.hasPath || challenge.proof.hasUp);

            uint leastEon = balances[1].eon;
            bytes32 key = ByteUtilLib.address2hash(msg.sender);
            //bytes32 key = challenge.proof.proof.leaf.nodeInfo.addressHash;
            if(challenge.proof.hasPath) {//Special case:eon-1 exist a account, evil owner removed it in this eon, so the account has only path
                uint tmpEon =  challenge.proof.proof.leaf.update.upData.eon;
                require(tmpEon >= 0);
                require(balances[1].hasRoot);
                require(tmpEon == leastEon);

                //require(key == challenge.proof.proof.leaf.nodeInfo.addressHash);

                ModelLib.HubRoot memory root = balances[1].root;
                bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(root.node, challenge.proof.proof);
                require(proofFlag);
            } else {//once the account had deposit, path must exist
                GlobleLib.Deposit memory d = all[msg.sender];
                require(!d.hasVal);
            }

            if(challenge.proof.hasUp) {//Special case:new account only update
                ModelLib.Update memory up = challenge.proof.update;

                uint tmpEon = up.upData.eon;
                require(tmpEon >= 0);
                require(tmpEon == leastEon);

                //require(msg.sender == ByteUtilLib.pubkey2Address(challenge.publicKey));//TODO

                bool signFlag = ModelLib.verifySign4Update(up.upData, up.sign, msg.sender);
                require(signFlag);
                bool hubSignFlag = ModelLib.verifySign4Update(up.upData, up.hubSign, owner);
                require(hubSignFlag);
            } else {

            }

            GlobleLib.BalanceUpdateChallengeAndStatus memory cs = balances[0].bucMeta.balanceChallenges[key];
            cs.challenge = data;
            cs.status = ModelLib.ChallengeStatus.OPEN;
            if(!cs.isVal) {
                balances[0].bucMeta.balanceChallengeKeys.push(key);
            }
            cs.isVal = true;
            balances[0].bucMeta.balanceChallenges[key] = cs;
            return true;
        }else {
            return false;
        }
    }

    function closeBalanceUpdateChallenge(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));

            ModelLib.HubRoot memory root = balances[1].root;
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(root.node, close.proof);
            require(proofFlag);

            bytes32 key = close.proof.leaf.addressHash;

            GlobleLib.BalanceUpdateChallengeAndStatus storage tmpStat = balances[0].bucMeta.balanceChallenges[key];
            require(tmpStat.isVal);
            ModelLib.BalanceUpdateChallengeStatus memory stat = GlobleLib.change2BalanceUpdateChallengeStatus(tmpStat);

            address addr = ByteUtilLib.pubkey2Address(stat.challenge.publicKey);
            bytes32 hash = ByteUtilLib.address2hash(addr);
            if(stat.status == ModelLib.ChallengeStatus.OPEN) {
                uint d = balances[1].depositMeta.deposits[hash];
                if(close.update.upData.sendAmount == 0 && close.update.upData.receiveAmount == 0) {
                    if(d > 0) {
                        require(d <= close.proof.path.leaf.allotment);
                    }
                } else {
                    bool signFlag = ModelLib.verifySign4Update(close.update.upData, close.update.sign, addr);
                    require(signFlag);
                    bool hubsignFlag = ModelLib.verifySign4Update(close.update.upData, close.update.hubSign, owner);
                    require(hubsignFlag);
                    require(close.update.upData.version >= stat.challenge.proof.update.upData.version);
                }

                uint preAllotment = 0;
                if(stat.challenge.proof.hasPath) {
                    preAllotment = stat.challenge.proof.proof.path.leaf.allotment;
                }

                uint t1 = SafeMath.add(close.update.upData.receiveAmount, preAllotment);
                t1 = SafeMath.add(t1, d);
                GlobleLib.Withdrawal memory w = balances[1].withdrawalMeta.withdrawals[hash];
                if(w.isVal) {
                    ModelLib.WithdrawalInfo memory info = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(w.info, true));
                    t1 = SafeMath.sub(t1, info.amount);
                }
                uint t2 = close.update.upData.sendAmount;
                uint allotment = SafeMath.sub(t1, t2);
                require(allotment == close.proof.path.leaf.allotment);

                //TODO: require(close.update == close.proof.leaf.nodeInfo.update);

                tmpStat.status == ModelLib.ChallengeStatus.CLOSE;
                balances[0].bucMeta.balanceChallenges[key] = tmpStat;
                emit SiriusEvent(key, 2, tmpStat.challenge);
            }
            return true;
        } else {
            return false;
        }
    }

    function openTransferDeliveryChallenge(bytes calldata data) external recovery returns (bool) {
        if(!recoveryMode) {
            ModelLib.TransferDeliveryChallenge memory open = ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
            require(open.update.upData.eon >= 0 && open.update.upData.eon == balances[1].eon);
            require(open.tran.offData.eon >= 0 && open.tran.offData.eon == balances[1].eon);

            bool signFlag = ModelLib.verifySign4Update(open.update.upData, open.update.hubSign, owner);
            require(signFlag);

            bytes32 hash = ModelLib.hash4OffchainTransaction(open.tran);
            bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(open.update.upData.root, open.path, hash);
            require(verifyFlag);

            GlobleLib.TransferDeliveryChallengeAndStatus memory challenge = balances[0].tdcMeta.transferChallenges[hash];
            challenge.challenge = data;
            challenge.stat = ModelLib.ChallengeStatus.OPEN;
            if(!challenge.isVal) {
                balances[0].tdcMeta.transferChallengeKeys.push(hash);
            }
            challenge.isVal = true;
            balances[0].tdcMeta.transferChallenges[hash] = challenge;
            return true;
        } else {
            return false;
        }
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external onlyOwner returns (bool) {
        if(!recoveryMode) {
            ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));

            bytes32 key = close.txHash;

            ModelLib.HubRoot memory latestRoot = latestRoot();
            bool proofFlag = ModelLib.verifyMembershipProof4AMTreeProof(latestRoot.node, close.proof);
            require(proofFlag);

            GlobleLib.TransferDeliveryChallengeAndStatus memory challenge = balances[0].tdcMeta.transferChallenges[key];
            require(challenge.isVal);

            if(challenge.stat == ModelLib.ChallengeStatus.OPEN) {
                address addr = ByteUtilLib.pubkey2Address(close.fromPublicKey);
                bool signFlag = ModelLib.verifySign4Update(close.update.upData, close.update.sign, addr);
                require(signFlag);

                //TODO:require(close.proof.leaf.nodeInfo.update == close.update);

                //bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(close.update.upData.root, close.txPath, close.txHash);
                //require(verifyFlag);

                challenge.stat = ModelLib.ChallengeStatus.CLOSE;
                balances[0].tdcMeta.transferChallenges[key] = challenge;
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

        uint amount = SafeMath.add(proof.path.leaf.allotment, balances[0].depositMeta.deposits[key]);
        amount = SafeMath.add(amount, balances[1].depositMeta.deposits[key]);
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
        bytes32 key = ByteUtilLib.address2hash(msg.sender);
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = balances[i].withdrawalMeta.withdrawals[key];
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
        bytes32 key = ByteUtilLib.address2hash(msg.sender);
        for (uint i=0; i < balances.length; i++) {
            if(balances[i].eon == eon) {
                tmp = balances[i].bucMeta.balanceChallenges[key];
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
                tmp = balances[i].tdcMeta.transferChallenges[txHash];
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
        GlobleLib.DepositMeta memory depositMeta;
        GlobleLib.WithdrawalMeta memory withdrawalMeta;
        ModelLib.HubRoot memory root;
        GlobleLib.TransferDeliveryChallengeMeta memory tdc;
        GlobleLib.BalanceUpdateChallengeMeta memory buc;
        return GlobleLib.Balance(newEon, false, root, depositMeta, withdrawalMeta, buc, tdc);
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

    function withdrawalProcessing(bytes32 key) private view returns (bool flag) {
        flag = false;

        for(uint i=0;i<balances.length;i++) {
            GlobleLib.Withdrawal memory with = balances[i].withdrawalMeta.withdrawals[key];

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
        require(!recoveryMode);

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
