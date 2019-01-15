package org.starcoin.sirius.wallet.core.blockchain

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.Hub

class BlockChainListener<T : ChainTransaction, A : ChainAccount>(hub: Hub<T,A>) {

    private val hub = hub

    fun onNewBlock(blockInfo: Block<*>) {
        blockInfo.transactions.forEach {
            //if (it.from?.equals(hub.walletAddress)!! && it.to?.equals(hub.contractAddress)!!) {
                //TODO
                //if (it.action == null) { // 没有action就是正常转账
                //    this.hub.hubStatus.confirmDeposit(it)
                //}
            //}
//            if (it.action.equals("") //TODO
//            ) {
//                // 从hub取钱到链上 init withdrawal request
//                // withdrawal
//            }
        }


    }
}
