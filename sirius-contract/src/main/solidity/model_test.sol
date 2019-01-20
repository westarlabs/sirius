pragma solidity ^0.5.1;

import "./lib/rlp_decoder.sol";
import "./lib/rlp_encoder.sol";
import "./lib/log_util.sol";
import "./lib/safe_math.sol";
import "./lib/model.sol";

contract test_all_interface {
    function hub_root_test_decode() external;
    function hub_root_test(bytes calldata data) external returns (bytes memory);
    function initiate_withdrawal_test(bytes calldata data) external returns (bytes memory);
    function cancel_withdrawal_test(bytes calldata data) external returns (bytes memory);
    function balance_update_challenge_test(bytes calldata data) external returns (bytes memory);
    function close_balance_update_challenge_test(bytes calldata data) external returns (bytes memory);
    function open_transfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory);
    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test(bytes calldata data) external returns (bytes memory);
    function am_tree_path_leaf_node_test(bytes calldata data) external returns (bytes memory);
    function balance_update_proof_test(bytes calldata data) external returns (bytes memory);
    function update_data_test(bytes calldata data) external returns (bytes memory);
    function update_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test2(bytes calldata data1, bytes calldata data2) external returns (bytes memory);
}

contract test_all is test_all_interface {

    function hub_root_test_decode() external {
        ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(hex"f852f84bf843a0123a1d14be2941b9692aaf935e49294d9e7af3849521f5f522628c244de06f3880a08d0da8cbfc71a73b24e599088e31641d292d6e6aba69aa0e3bb328fcf10659a4808083d26998840113b1c7", true));

        Log.log("hub_root_test_d_eon", root.eon);
        Log.log("hub_root_test_d_offset", root.node.offset);
        Log.log("hub_root_test_d_allotment", root.node.allotment);
        Log.log("hub_root_test_d_node_left", root.node.nodeInfo.left);
        Log.log("hub_root_test_d_node_offset", root.node.nodeInfo.offset);
        Log.log("hub_root_test_d_node_right", root.node.nodeInfo.right);
    }

    function hub_root_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(RLPDecoder.toRLPItem(data, true));
        return ModelLib.marshalHubRoot(root);
    }

    function initiate_withdrawal_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.WithdrawalInfo memory init = ModelLib.unmarshalWithdrawalInfo(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalWithdrawalInfo(init);
    }

    function cancel_withdrawal_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CancelWithdrawal memory cancel = ModelLib.unmarshalCancelWithdrawal(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalCancelWithdrawal(cancel);
    }

    function balance_update_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.BalanceUpdateChallenge memory challenge = ModelLib.unmarshalBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalBalanceUpdateChallenge(challenge);
    }

    function close_balance_update_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalCloseBalanceUpdateChallenge(close);
    }

    function open_transfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.TransferDeliveryChallenge memory open = ModelLib.unmarshalTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalTransferDeliveryChallenge(open);
    }

    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalCloseTransferDeliveryChallenge(close);
    }

    function am_tree_proof_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreeProof(proof);
    }

    function am_tree_path_leaf_node_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreePathLeafNode memory leaf = ModelLib.unmarshalAMTreePathLeafNode(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreePathLeafNode(leaf);
    }

    function balance_update_proof_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.BalanceUpdateProof memory proof = ModelLib.unmarshalBalanceUpdateProof(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalBalanceUpdateProof(proof);
    }

    function contract_return_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.ContractReturn memory cr = ModelLib.unmarshalContractReturn(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalContractReturn(cr);
    }

    function update_data_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.UpdateData memory ud = ModelLib.unmarshalUpdateData(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalUpdateData(ud);
    }

    function update_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.Update memory u = ModelLib.unmarshalUpdate(RLPDecoder.toRLPItem(data, true));
        return ModelLib.marshalUpdate(u);
    }

    function am_tree_proof_test2(bytes calldata data1, bytes calldata data2) external returns (bytes memory) {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data1, true));
        ModelLib.AMTreePathInternalNode memory root = ModelLib.unmarshalAMTreePathInternalNode(RLPDecoder.toRLPItem(data2, true));

        bool flag = verifyMembershipProof4AMTreeProof(root, proof);
        Log.log("flag", flag);
        return data2;
    }

    function verifyMembershipProof4AMTreeProof(ModelLib.AMTreePathInternalNode memory root, ModelLib.AMTreeProof memory proof) internal returns(bool) {
        ModelLib.AMTreePath memory path = proof.path;
        ModelLib.AMTreePathLeafNode memory leaf = proof.leaf;

        //AMTreePathLeafNode -> AMTreeInternalNodeInfo
        ModelLib.AMTreeInternalNodeInfo memory nodeInfo;
        bytes32 leafHash = keccak256(ModelLib.marshalAMTreePathLeafNode(leaf));//;keccak256(ModelLib.amTreePathLeafNode2Bytes(leaf));
        bytes32 pathLeafHash = keccak256(ModelLib.marshalAMTreePathLeafNode(path.leaf));

        uint allotment = SafeMath.add(leaf.allotment, path.leaf.allotment);
        uint offset;
        if(leaf.direction == ModelLib.Direction.DIRECTION_LEFT) {
            nodeInfo.left = leafHash;
            nodeInfo.offset = SafeMath.add(leaf.offset, leaf.allotment);
            nodeInfo.right = pathLeafHash;
            offset = leaf.offset;
        } else {
            nodeInfo.left = pathLeafHash;
            nodeInfo.offset = SafeMath.add(path.leaf.offset, path.leaf.allotment);
            nodeInfo.right = leafHash;
            offset = path.leaf.offset;
        }

        //AMTreeInternalNodeInfo -> AMTreePathInternalNode
        ModelLib.AMTreePathInternalNode memory computeNode;
        computeNode.nodeInfo = nodeInfo;
        computeNode.offset = offset;
        computeNode.allotment = allotment;
        printAMTreeInternalNodeInfo(computeNode);

        for (uint i=0;i<path.nodes.length;i++) {
            ModelLib.AMTreePathInternalNode memory node = path.nodes[i];
            if (node.direction == ModelLib.Direction.DIRECTION_LEFT) {
                computeNode.direction = ModelLib.Direction.DIRECTION_RIGHT;
                computeNode = ModelLib.combineAMTreePathInternalNode(node, computeNode);
                printAMTreeInternalNodeInfo(computeNode);
            } else if(node.direction == ModelLib.Direction.DIRECTION_RIGHT) {
                computeNode.direction = ModelLib.Direction.DIRECTION_LEFT;
                computeNode = ModelLib.combineAMTreePathInternalNode(computeNode, node);
                printAMTreeInternalNodeInfo(computeNode);
            } else {}
        }
        computeNode.direction = ModelLib.Direction.DIRECTION_ROOT;

        //AMTreePathInternalNode -> sha256
        Log.log("hub",keccak256(ModelLib.marshalAMTreePathInternalNode(root)));
        Log.log("hub",keccak256(ModelLib.marshalAMTreePathInternalNode(computeNode)));
        return (keccak256(ModelLib.marshalAMTreePathInternalNode(root)) == keccak256(ModelLib.marshalAMTreePathInternalNode(computeNode)) && root.offset == computeNode.offset && root.allotment == computeNode.allotment);
    }

    function printAMTreeInternalNodeInfo(ModelLib.AMTreePathInternalNode memory node) internal {
        Log.log("hub", node.offset);
        Log.log("hub", node.nodeInfo.left);
        Log.log("hub", node.nodeInfo.offset);
        Log.log("hub", node.nodeInfo.right);
        Log.log("hub", node.allotment);
    }
}
