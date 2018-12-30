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
    function open_pransfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory);
    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test(bytes calldata data) external returns (bytes memory);
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

    function open_pransfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.OpenTransferDeliveryChallengeRequest memory open = ModelLib.unmarshalOpenTransferDeliveryChallengeRequest(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalOpenTransferDeliveryChallengeRequest(open);
    }

    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalCloseTransferDeliveryChallenge(close);
    }

    function am_tree_proof_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreeProof(proof);
    }
}
