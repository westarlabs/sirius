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
    function open_pransfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory);
    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test(bytes calldata data) external returns (bytes memory);
}

contract test_all is test_all_interface {

    function hub_root_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.HubRoot memory root = ModelLib.unmarshalHubRoot(data);

        return ModelLib.marshalHubRoot(root);
    }

    function initiate_withdrawal_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.WithdrawalInfo memory init = ModelLib.unmarshalWithdrawalInfo(data);

        return ModelLib.marshalWithdrawalInfo(init);
    }

    function cancel_withdrawal_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CancelWithdrawal memory cancel = ModelLib.unmarshalCancelWithdrawal(data);

        return ModelLib.marshalCancelWithdrawal(cancel);
    }

    function balance_update_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.BalanceUpdateChallenge memory challenge = ModelLib.unmarshalBalanceUpdateChallenge(data);

        return ModelLib.marshalBalanceUpdateChallenge(challenge);
    }

    function close_balance_update_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CloseBalanceUpdateChallenge memory close = ModelLib.unmarshalCloseBalanceUpdateChallenge(data);

        return ModelLib.marshalCloseBalanceUpdateChallenge(close);
    }

    function open_pransfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.OpenTransferDeliveryChallengeRequest memory open = ModelLib.unmarshalOpenTransferDeliveryChallengeRequest(data);

        return ModelLib.marshalOpenTransferDeliveryChallengeRequest(open);
    }

    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(data);

        return ModelLib.marshalCloseTransferDeliveryChallenge(close);
    }

    function am_tree_proof_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreeProof memory proof = ModelLib.unmarshalAMTreeProof(data);

        return ModelLib.marshalAMTreeProof(proof);
    }
}
