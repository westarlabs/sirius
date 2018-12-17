pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
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

library ChainHashLib {
    struct ChainHash {
        byte32 hash;
    }
}

library NodeInfoLib {
    struct NodeInfo {
        bytes32 left;
        uint amount;
        bytes32 right;
    }

    //rlp byte to object
    function marshal(bytes data) internal pure returns (NodeInfo memory root) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);

        RLPDecoder.Iterator memory it = rlp.iterator();
        uint idx;
        while (it.hasNext()) {
            RLPDecoder.RLPItem memory r = it.next();
//            if (idx == 0) root.offset =;
//            else if (idx == 1) Log.log("testStruct:2:", r.toAddress());
//            else if (idx == 2) Log.log("testStruct:3:", r.toUint());
//            else if (idx == 3) Log.log("testStruct:4:", r.toAscii());

            idx++;
        }
    }

    //object to rlp byte
    function unmarshal() internal pure returns (bytes memory) {

    }
}

library HubRootNodeLib {
    struct HubRootNode {
        uint offset;
        uint allotment;
        NodeInfoLib.NodeInfo node;
    }

}

library HubRootLib {
    struct HubRoot {
        HubRootNodeLib.HubRootNode node;
        uint eon;
    }

    //rlp byte to object
    function marshal(bytes data) internal pure returns (HubRoot memory root) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);

        RLPDecoder.Iterator memory it = rlp.iterator();
        uint idx;
        while (it.hasNext()) {
            RLPDecoder.RLPItem memory r = it.next();
//            if (idx == 0) root.offset =;
//            else if (idx == 1) Log.log("testStruct:2:", r.toAddress());
//            else if (idx == 2) Log.log("testStruct:3:", r.toUint());
//            else if (idx == 3) Log.log("testStruct:4:", r.toAscii());

            idx++;
        }
    }

    //object to rlp byte
    function unmarshal() internal pure returns (bytes memory) {

    }
}
