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
    function verify_proof_test(bytes calldata data1, bytes calldata data2) external returns (bool);
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

    function contract_return_test(bytes calldata data) external pure returns (bytes memory) {
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

    function verify_proof_test(bytes calldata data1, bytes calldata data2) external returns (bool) {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(RLPDecoder.toRLPItem(data1, true));
        ModelLib.AMTreePathNode memory root = ModelLib.unmarshalAMTreePathNode(RLPDecoder.toRLPItem(data2, true));

        return ModelLib.verifyMembershipProof4AMTreeProof(root, proof);
    }
}
