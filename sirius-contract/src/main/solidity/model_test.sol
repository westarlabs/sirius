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
    function open_transfer_delivery_challenge_request_test(bytes calldata data) external returns (bytes memory);
    function close_transfer_delivery_challenge_test(bytes calldata data) external returns (bytes memory);
    function am_tree_path_node_test(bytes calldata data) external returns (bytes memory);
    function am_tree_path_test(bytes calldata data) external returns (bytes memory);
    function am_tree_proof_test(bytes calldata data) external returns (bytes memory);
    function balance_update_proof_test(bytes calldata data) external returns (bytes memory);
    function balance_update_proof_test2(bytes calldata data) external returns (bytes memory);
    function update_data_test(bytes calldata data) external returns (bytes memory);
    function update_test(bytes calldata data) external returns (bytes memory);
    function verify_proof_test(bytes calldata data1, bytes calldata data2) external returns (bool);
    function verify_merkle_test(bytes calldata data) external returns (bool);
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

    function am_tree_path_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreePath memory path = ModelLib.unmarshalAMTreePath(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreePath(path);
    }

    function am_tree_path_node_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.AMTreePathNode memory leaf = ModelLib.unmarshalAMTreePathNode(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalAMTreePathNode(leaf);
    }

    function balance_update_proof_test(bytes calldata data) external returns (bytes memory) {
        ModelLib.BalanceUpdateProof memory proof = ModelLib.unmarshalBalanceUpdateProof(RLPDecoder.toRLPItem(data, true));

        return ModelLib.marshalBalanceUpdateProof(proof);
    }

    function balance_update_proof_test2(bytes calldata data) external returns (bytes memory) {
        RLPLib.RLPItem memory rlp = RLPDecoder.toRLPItem(data, true);
        RLPLib.Iterator memory it = RLPDecoder.iterator(rlp);

        uint idx;
        ModelLib.BalanceUpdateProof memory bup;
        while(RLPDecoder.hasNext(it)) {
            RLPLib.RLPItem memory r = RLPDecoder.next(it);

            if(idx == 0) bup.hasUp = RLPDecoder.toBool(r);
            else if(idx == 1) {
                RLPLib.Iterator memory it1 = RLPDecoder.iterator(r);
                ModelLib.Update memory update;
                uint idx1;
                while (RLPDecoder.hasNext(it1)) {
                    RLPLib.RLPItem memory r2 = RLPDecoder.next(it1);

                    if (idx1 == 0) {
                        update.upData = ModelLib.unmarshalUpdateData(r2);
                        Log.log("44", ModelLib.marshalUpdateData(update.upData));
                    }else if (idx1 == 1) {
                        RLPLib.RLPItem memory tmp = RLPDecoder.toRLPItem(RLPLib.toData(r2), true);
                        RLPLib.Iterator memory it3 = RLPDecoder.iterator(tmp);
                        ModelLib.Signature memory sign;
                        uint idx3;
                        while(RLPDecoder.hasNext(it3)) {
                            RLPLib.RLPItem memory r3 = RLPDecoder.next(it3);
                            if(idx3 == 0) {
                                bytes memory bs = RLPLib.toData(r3);
                                sign.v = bs[0];
                            } else if(idx3 == 1) {
                                sign.r = ByteUtilLib.bytesToBytes32(RLPLib.toData(r3));
                            } else if(idx3 == 2) {
                                sign.s = ByteUtilLib.bytesToBytes32(RLPLib.toData(r3));
                             } else {}

                            idx3++;
                        }
                        Log.log("33", ModelLib.marshalSignature(sign));
                        update.sign = sign;
                    } else if (idx1 == 2) update.hubSign = ModelLib.unmarshalSignature(r2);
                    else {}

                    idx1++;
                }

                Log.log("22", ModelLib.marshalUpdate(update));
                bup.update = update;
            } else if(idx == 2) bup.hasPath = RLPDecoder.toBool(r);
            else if(idx == 3) bup.path = ModelLib.unmarshalAMTreePath(r);
            else {}

            idx++;
        }

        Log.log("11", ModelLib.marshalBalanceUpdateProof(bup));
        return data;
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

    function verify_merkle_test(bytes calldata data) external returns (bool) {
        ModelLib.CloseTransferDeliveryChallenge memory close = ModelLib.unmarshalCloseTransferDeliveryChallenge(RLPDecoder.toRLPItem(data, true));
        bool verifyFlag = ModelLib.verifyMembershipProof4Merkle(close.proof.leaf.update.upData.root, close.txPath, close.txHash);

        return verifyFlag;
    }
}
