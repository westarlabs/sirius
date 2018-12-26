package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin

data class TransferDeliveryChallenge(
    var update: UpdateData = UpdateData.DUMMY_UPDATE_DATA,
    var transaction: OffchainTransaction = OffchainTransaction.DUMMY_OFFCHAIN_TRAN,
    var path: MerklePath = MerklePath()
) : SiriusObject() {
    companion object : SiriusObjectCompanion<TransferDeliveryChallenge, Starcoin.ProtoTransferDeliveryChallenge>(
        TransferDeliveryChallenge::class
    ) {

        var DUMMY_TRAN_DELIVERY_CHALLENGE = TransferDeliveryChallenge()

        override fun mock(): TransferDeliveryChallenge {
            return TransferDeliveryChallenge(UpdateData.mock(), OffchainTransaction.mock(), MerklePath.mock())
        }
    }
}
