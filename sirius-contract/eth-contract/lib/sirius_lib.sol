pragma solidity ^0.5.1;

import "./sirius_pb.sol";

library WithdrawalLib {

    using SafeMath for uint;

    struct WithdrawalMeta {
        uint total;
        mapping(address => uint) withdrawals;
    }
}

library DepositLib {

    using SafeMath for uint;

    struct DepositMeta {
        uint total;
        mapping(address => uint) deposits;
    }

    function add(DepositMeta storage self, address addr, uint amount) internal {
        self.total = self.total.add(amount);
        self.deposits[addr] = self.deposits[addr].add(amount);
    }
}

library BalanceLib {
    struct Balance {
        uint eon;
        DepositLib.DepositMeta depositMeta;
        WithdrawalLib.WithdrawalMeta withdrawalMeta;
        HubRootLib.HubRoot root;
        bool hasRoot;
    }
}

library NodeInfoLib {
    struct NodeInfo {
        bytes32 left;
        uint amount;
        bytes32 right;
    }
}

library HubRootLib {
    struct HubRoot {
        NodeInfoLib.NodeInfo node;
        uint offset;
        uint allotment;
        uint eon;
    }
}

library SafeMath {
    function mul(uint256 a, uint256 b) public pure returns (uint256) {
        uint256 c = a * b;
        assert(a == 0 || c / a == b);
        return c;
    }

    function div(uint256 a, uint256 b) public pure returns (uint256) {
        assert(b > 0);
        uint256 c = a / b;
        return c;
    }

    function sub(uint256 a, uint256 b) public pure returns (uint256) {
        assert(b <= a);
        return a - b;
    }

    function add(uint256 a, uint256 b) public pure returns (uint256) {
        uint256 c = a + b;
        assert(c >= a);
        return c;
    }
}
