pragma solidity ^0.5.1;

import "./lib/rlp_decoder.sol";
import "./lib/rlp_encoder.sol";
import "./lib/log_util.sol";
import "./lib/safe_math.sol";
import "./lib/model.sol";

contract test_all_interface {
    function hub_root_test(bytes calldata data) external returns (bytes memory);
    function initiate_withdrawal_test(bytes calldata data) external returns (bytes memory);
    function cancel_withdrawal_test(bytes calldata data) external returns (bytes memory);
    function balance_update_challenge_test(bytes calldata data) external returns (bytes memory);
    function close_balance_update_challenge_test(bytes calldata data) external returns (bytes memory);
    function open_transfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory);
    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory);
    function am_tree_path_node_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test(bytes calldata data) external returns (bytes memory);
    function balance_update_proof_test(bytes calldata data) external returns (bytes memory);
    function update_data_test(bytes calldata data) external returns (bytes memory);
    function update_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test2(bytes calldata data1, bytes calldata data2) external returns (bytes memory);
}

contract test_all is test_all_interface {

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

    function am_tree_path_node_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreePathNode memory leaf = ModelLib.unmarshalAMTreePathNode(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreePathNode(leaf);
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
        ModelLib.AMTreePathNode memory root = ModelLib.unmarshalAMTreePathNode(RLPDecoder.toRLPItem(data2, true));

        bool flag = verifyMembershipProof4AMTreeProof(root, proof);
        Log.log("flag", flag);
        return data2;
    }

    function verifyMembershipProof4AMTreeProof(ModelLib.AMTreePathNode memory root, ModelLib.AMTreeProof memory proof) internal returns(bool) {
        //AMTreeLeafNodeInfo == AMTreePath.leaf
        bytes32 leafHash = keccak256(ModelLib.marshalAMTreeLeafNodeInfo(proof.leaf));
        require(leafHash == proof.path.leaf.nodeHash);

        //AMTreePath -> root
        ModelLib.AMTreePathNode memory computeNode = proof.path.leaf;
        for (uint i=0;i<proof.path.nodes.length;i++) {
            ModelLib.AMTreePathNode memory node = proof.path.nodes[i];
            if (node.direction == ModelLib.Direction.DIRECTION_LEFT) {
                computeNode.direction = ModelLib.Direction.DIRECTION_RIGHT;
                computeNode = combineAMTreePathNode(node, computeNode);
                //printAMTreeInternalNodeInfo(computeNode);
            } else if(node.direction == ModelLib.Direction.DIRECTION_RIGHT) {
                computeNode.direction = ModelLib.Direction.DIRECTION_LEFT;
                computeNode = combineAMTreePathNode(computeNode, node);
                //printAMTreeInternalNodeInfo(computeNode);
            } else {}
        }

        computeNode.direction = ModelLib.Direction.DIRECTION_ROOT;

        //hash == hash
        return (keccak256(ModelLib.marshalAMTreePathNode(root)) == keccak256(ModelLib.marshalAMTreePathNode(computeNode)) && root.offset == computeNode.offset && root.allotment == computeNode.allotment);
    }

    function combineAMTreePathNode(ModelLib.AMTreePathNode memory left, ModelLib.AMTreePathNode memory right) internal returns (ModelLib.AMTreePathNode memory node) {
        ModelLib.AMTreeInternalNodeInfo memory tmp;
        tmp.left = left.nodeHash;
        tmp.offset = right.offset;
        tmp.right = right.nodeHash;

        bytes32 nodeHash = keccak256(ModelLib.marshalAMTreeInternalNodeInfo(tmp));

        node.nodeHash = nodeHash;
        node.offset = left.offset;
        node.allotment = SafeMath.add(left.allotment, right.allotment);
    }
}
