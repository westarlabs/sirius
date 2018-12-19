pragma solidity ^0.5.1;

import "../lib/rlp_decoder.sol";
import "../lib/rlp_encoder.sol";
import "../lib/log_util.sol";
import "../lib/safe_math.sol";
import "../lib/model.sol";

contract test_all_interface {
    function chain_hash_test() external;
    function node_info_test(uint amount) external;
    function hub_root_node_test(uint amount, uint offset, uint allotment) external;
    function hub_root_test(uint amount, uint offset, uint allotment, uint eon) external;
    function test( uint amount) external;
}

contract test_all is test_all_interface {

    function chain_hash_data() private pure returns (ChainHashLib.ChainHash memory chainHash) {
        chainHash.hash = 0x8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92;
    }

    function chain_hash_test() external {
        ChainHashLib.ChainHash memory chainHash = chain_hash_data();
        bytes memory bs = ChainHashLib.unmarshal(chainHash);
        ChainHashLib.ChainHash memory tmp = ChainHashLib.marshal(bs);
        assert(chainHash.hash == tmp.hash);
    }

    function node_info_data(uint amount) private pure returns (NodeInfoLib.NodeInfo memory nodeInfo) {
        ChainHashLib.ChainHash memory left = chain_hash_data();
        ChainHashLib.ChainHash memory right = chain_hash_data();
        nodeInfo.amount = amount;
        nodeInfo.left = left;
        nodeInfo.right = right;
    }

    function node_info_test(uint amount) external {
        NodeInfoLib.NodeInfo memory nodeInfo = node_info_data(amount);
        bytes memory bs = NodeInfoLib.unmarshal(nodeInfo);

        NodeInfoLib.NodeInfo memory tmp = NodeInfoLib.marshal(bs);
        Log.log("a", tmp.left.hash);
        Log.log("b", tmp.amount);
        assert(nodeInfo.left.hash == tmp.left.hash);
        assert(nodeInfo.amount == tmp.amount);
    }

    function hub_root_node_data(uint amount, uint offset, uint allotment) private pure returns (HubRootNodeLib.HubRootNode memory rootNode) {
        NodeInfoLib.NodeInfo memory nodeInfo = node_info_data(amount);
        rootNode.offset = offset;
        rootNode.allotment = allotment;
        rootNode.info = nodeInfo;
    }

    function hub_root_node_test(uint amount, uint offset, uint allotment) external {
        HubRootNodeLib.HubRootNode memory rootNode = hub_root_node_data(amount, offset, allotment);
        bytes memory bs = HubRootNodeLib.unmarshal(rootNode);

        HubRootNodeLib.HubRootNode memory tmp = HubRootNodeLib.marshal(bs);
        Log.log("a", tmp.info.left.hash);
        Log.log("b", tmp.info.amount);

        assert(rootNode.offset == tmp.offset);
        assert(rootNode.allotment == tmp.allotment);
        assert(rootNode.info.left.hash == tmp.info.left.hash);
        assert(rootNode.info.amount == tmp.info.amount);
    }

    function hub_root_data(uint amount, uint offset, uint allotment, uint eon) private pure returns (HubRootLib.HubRoot memory root) {
        HubRootNodeLib.HubRootNode memory rootNode = hub_root_node_data(amount, offset, allotment);
        root.node = rootNode;
        root.eon = eon;
    }

    function hub_root_test(uint amount, uint offset, uint allotment, uint eon) external {
        HubRootLib.HubRoot memory root = hub_root_data(amount, offset, allotment, eon);
        bytes memory bs = HubRootLib.unmarshal(root);

        HubRootLib.HubRoot memory tmp = HubRootLib.marshal(bs);
        Log.log("a", tmp.node.info.left.hash);
        Log.log("b", tmp.node.info.amount);

        assert(root.eon == tmp.eon);
        assert(root.node.info.left.hash == tmp.node.info.left.hash);
        assert(root.node.info.amount == tmp.node.info.amount);
    }
    function test(uint amount) external {
        ChainHashLib.ChainHash memory left;
        left.hash = 0x8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92;

        ChainHashLib.ChainHash memory right;
        right.hash = 0x8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92;
        NodeInfoLib.NodeInfo memory nodeInfo;
        nodeInfo.amount = amount;
        nodeInfo.left = left;
        nodeInfo.right = right;
        bytes memory bs = NodeInfoLib.unmarshal(nodeInfo);

        Log.log("aaa", bs);
        RLPDecoder.RLPItem memory rlp = RLPDecoder.toRLPItem(bs);
        RLPDecoder.Iterator memory it = RLPDecoder.iterator(rlp);
        uint a = RLPDecoder.items(rlp);
        Log.log("a", a);
        RLPDecoder.RLPItem memory r = RLPDecoder.next(it);
        NodeInfoLib.NodeInfo memory tmp;
        tmp.left = ChainHashLib.marshal(RLPDecoder.toData(r));
        Log.log("a", tmp.left.hash);

        r = RLPDecoder.next(it);
        tmp.amount = RLPDecoder.toUint(r);
        Log.log("a", tmp.amount);

        r = RLPDecoder.next(it);
        tmp.right = ChainHashLib.marshal(RLPDecoder.toData(r));
        Log.log("c",tmp.right.hash);
    }
}
