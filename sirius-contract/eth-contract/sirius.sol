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
}

contract SiriusService is Sirius {
    address private owner;
    bool private recoveryMode = false;
    uint private startHeight;
    uint private blocksPerEon;
    uint private balanceSize;
    BalanceLib.Balance[3] balances;

    using SafeMath for uint;
    using DepositLib for DepositLib.DepositMeta;

    event DepositEvent(address indexed addr, uint value);

    constructor(uint blocks) public {
        assert(blocks >= 4);
        blocksPerEon = blocks;
        owner = msg.sender;
        startHeight = block.number;
    }

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

    function deposit() external payable recovery {
        assert(msg.value > 0);
        DepositLib.add(balances[0].depositMeta, msg.sender, msg.value);
        emit DepositEvent(msg.sender, msg.value);
    }

    function commit(bytes calldata data) external recovery {
        HubRootLib.HubRoot memory root = HubRootLib.unmarshal(data);
        assert(!balances[0].hasRoot);
        assert(root.eon > 0);
        assert(balances[0].eon == root.eon);
        assert(root.node.allotment >= 0);
        uint tmp = SafeMath.add(balances[1].root.node.allotment, balances[1].depositMeta.total);
        uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalMeta.total);
        assert(allotmentTmp >= 0 && allotmentTmp == root.node.allotment);
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
            } else if (balances[i - 1].hasRoot)
                balances[i] = balances[i - 1];
        }
    }

    function initiateWithdrawal(bytes calldata data) external recovery {
        InitiateWithdrawalRequestLib.InitiateWithdrawalRequest memory init = InitiateWithdrawalRequestLib.unmarshal(data);
        init;
        //TODO
    }

    function cancelWithdrawal(bytes calldata data) external recovery {
        CancelWithdrawalRequestLib.CancelWithdrawalRequest memory cancel = CancelWithdrawalRequestLib.unmarshal(data);
        cancel;
        //TODO
    }

    function openBalanceUpdateChallenge(bytes calldata data) external {
        BalanceUpdateChallengeLib.BalanceUpdateChallenge memory challenge = BalanceUpdateChallengeLib.unmarshal(data);
        challenge;
        //TODO
    }

    function closeBalanceUpdateChallenge(bytes calldata data) external {
        BalanceUpdateProofLib.BalanceUpdateProof memory proof = BalanceUpdateProofLib.unmarshal(data);
        proof;
        //TODO
    }

    function openTransferDeliveryChallenge(bytes calldata data) external {
        OpenTransferDeliveryChallengeRequestLib.OpenTransferDeliveryChallengeRequest memory open = OpenTransferDeliveryChallengeRequestLib.unmarshal(data);
        open;
        //TODO
    }

    function closeTransferDeliveryChallenge(bytes calldata data) external {
        CloseTransferDeliveryChallengeRequestLib.CloseTransferDeliveryChallengeRequest memory close = CloseTransferDeliveryChallengeRequestLib.unmarshal(data);
        close;
        //TODO
    }

    function recoverFunds(bytes calldata data) external {
        AugmentedMerklePathLib.AugmentedMerklePath memory path = AugmentedMerklePathLib.unmarshal(data);
        path;
        //TODO
    }

    function getLatestRoot() external returns (bytes memory) {
        //TODO
    }

    function getCurrentEon() external returns (uint) {
        //TODO
    }

    function isRecoveryMode() external returns (bool) {
        return recoveryMode;
    }
}