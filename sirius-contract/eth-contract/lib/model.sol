pragma solidity ^0.5.1;

import "./rlp_decoder.sol";
import "./rlp_encoder.sol";
import "./safe_math.sol";
import "./byte_util.sol";

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
        bytes32 hash;
    }

    function marshal(bytes memory data) internal pure returns (ChainHash memory chainHash) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);
        bytes memory tmp = RLPDecoder.toData(rlp);
        chainHash.hash = ByteUtilLib.bytesToBytes32(tmp);
        return chainHash;
    }

    function unmarshal(ChainHash memory self) internal pure returns (bytes memory) {
        bytes memory bs = new bytes(32);
        for (uint256 i; i < 32; i++) {
            bs[i] = self.hash[i];
        }

        bytes memory middle = RLPEncoder.encodeBytes(bs);
        return RLPEncoder.encodeBytes(middle);
    }
}

library NodeInfoLib {
    struct NodeInfo {
        ChainHashLib.ChainHash left;
        uint amount;
        ChainHashLib.ChainHash right;
    }

    function marshal(bytes memory data) internal pure returns (NodeInfo memory node) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);

        RLPDecoder.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPDecoder.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) node.left = ChainHashLib.marshal(RLPDecoder.toData(r));
            else if (idx == 1) node.amount = RLPDecoder.toUint(r);
            else if (idx == 2) node.right = ChainHashLib.marshal(RLPDecoder.toData(r));
            else {}

            idx++;
        }

        return node;
    }

    function unmarshal(NodeInfo memory node) internal pure returns (bytes memory) {
        bytes memory left = ChainHashLib.unmarshal(node.left);
        bytes memory amount = RLPEncoder.encodeUint(node.amount);
        bytes memory right = ChainHashLib.unmarshal(node.right);

        bytes memory middle = RLPEncoder.append(RLPEncoder.append(left, amount), right);
        return RLPEncoder.encodeBytes(middle);
    }
}

library HubRootNodeLib {
    struct HubRootNode {
        uint offset;
        uint allotment;
        NodeInfoLib.NodeInfo node;
    }

    function marshal(bytes memory data) internal pure returns (HubRootNode memory rootNode) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);

        RLPDecoder.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPDecoder.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) rootNode.offset = RLPDecoder.toUint(r);
            else if (idx == 1) rootNode.node = NodeInfoLib.marshal(RLPDecoder.toData(r));
            else if (idx == 2) rootNode.allotment = RLPDecoder.toUint(r);
            else {}

            idx++;
        }

        return rootNode;
    }

    function unmarshal(HubRootNode memory rootNode) internal pure returns (bytes memory) {
        bytes memory offset = RLPEncoder.encodeUint(rootNode.offset);
        bytes memory node = NodeInfoLib.unmarshal(rootNode.node);
        bytes memory allotment = RLPEncoder.encodeUint(rootNode.allotment);

        bytes memory middle = RLPEncoder.append(RLPEncoder.append(offset, node), allotment);
        return RLPEncoder.encodeBytes(middle);
    }
}

library HubRootLib {
    struct HubRoot {
        HubRootNodeLib.HubRootNode node;
        uint eon;
    }

    function marshal(bytes memory data) internal pure returns (HubRoot memory root) {
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(data);

        RLPDecoder.Iterator memory it = RLPDecoder.iterator(rlp);
        uint idx;
        while (RLPDecoder.hasNext(it)) {
            RLPDecoder.RLPItem memory r = RLPDecoder.next(it);
            if (idx == 0) root.node = HubRootNodeLib.marshal(RLPDecoder.toData(r));
            else if (idx == 1) root.eon = RLPDecoder.toUint(r);
            else {}

            idx++;
        }

        return root;
    }

    function unmarshal(HubRoot memory root) internal pure returns (bytes memory) {
        bytes memory node = HubRootNodeLib.unmarshal(root.node);
        bytes memory eon = RLPEncoder.encodeUint(root.eon);

        bytes memory middle = RLPEncoder.append(node, eon);
        return RLPEncoder.encodeBytes(middle);
    }
}
