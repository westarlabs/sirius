package org.starcoin.sirius.wallet.core.blockchain

import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.wallet.core.Hub

class BlockChainListener(hub: Hub) {

    private val hub = hub

    fun onNewBlock(blockInfo: BlockInfo){
        blockInfo.getTransactions().forEach {
            if (it.from?.equals(hub.walletAddress)!! && it.to?.equals(hub.contractAddress)!!) {
                if (it.action == null) { // 没有action就是正常转账
                    this.hub.hubStatus.confirmDeposit(it)
                }
            }
            if (it.action.equals("") //TODO
            ) {
                // 从hub取钱到链上 init withdrawal request
                // withdrawal
            }
        }


    }
}