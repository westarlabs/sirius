package org.starcoin.sirius.protocol.ethereum.contract

import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.HubContract

class InMemoryHubContract : HubContract{
    override fun queryHubInfo(): HubInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryLeastHubCommit(): AugmentedMerkleTree.AugmentedMerkleTreeNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryHubCommit(eon: Int): AugmentedMerkleTree.AugmentedMerkleTreeNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryCurrentBalanceUpdateChallenge(address: BlockAddress): BalanceUpdateChallenge {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryCurrentTransferDeliveryChallenge(address: BlockAddress): TransferDeliveryChallenge {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryWithdrawalStatus(address: BlockAddress): WithdrawalStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initiateWithdrawal(request: InitiateWithdrawal): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancelWithdrawal(request: CancelWithdrawal): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeBalanceUpdateChallenge(request: CloseBalanceUpdateChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun commit(request: HubRoot): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openTransferDeliveryChallenge(request: TransferDeliveryChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeTransferDeliveryChallenge(request: CloseTransferDeliveryChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recoverFunds(request: RecoverFunds): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}