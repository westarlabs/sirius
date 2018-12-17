pragma solidity ^0.5.1;

import "./safe_math.sol";

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
