pragma solidity ^0.5.1;

import "./lib/model.sol";
import "./lib/safe_math.sol";

interface Sirius {
    function deposit() external payable;

    function commit(uint allotment, uint eon, bytes32 left, uint amount, bytes32 right) external;
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

    function commit(uint allotment, uint eon, bytes32 left, uint amount, bytes32 right) external recovery {
        assert(!balances[0].hasRoot);
        assert(balances[0].eon == eon);
        assert(allotment >= 0);
        uint tmp = SafeMath.add(balances[1].root.allotment, balances[1].depositMeta.total);
        uint allotmentTmp = SafeMath.sub(tmp, balances[1].withdrawalMeta.total);
        assert(allotmentTmp >= 0 && allotmentTmp == allotment);
        balances[0].root.node.left = left;
        balances[0].root.node.amount = amount;
        balances[0].root.node.right = right;
        balances[0].root.allotment = allotment;
        balances[0].root.eon = eon;
        balances[0].hasRoot = true;
    }

    function checkBalances(BalanceLib.Balance memory latest) private {
        uint i = (balances.length - 1);
        for (; i >= 0; i--) {
            if (i == 0)
                balances[i] = latest;
            else if (balances[i - 1].hasRoot)
                balances[i] = balances[i - 1];
        }
    }
}