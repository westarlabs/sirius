package org.starcoin.sirius.wallet.core.blockchain

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.wallet.core.HubStatus

class BlockChain <T : ChainTransaction, A : ChainAccount> (chain: Chain<T, out Block<T>, A>,hubStatus: HubStatus,hubContract: HubContract<A>,account: A){

    private val chain = chain
    private val hubStatus = hubStatus
    private val contract = hubContract
    private val account = account
    internal var startWatch = false

    fun watchTransaction(){
        GlobalScope.launch {
            val channnel=chain.watchTransactions {
                it.tx.from?.equals(account.address) ?: false || it.tx.from?.equals(contract.contractAddress) ?: false
                        || it.tx.to?.equals(account.address) ?: false || it.tx.to?.equals(contract.contractAddress) ?: false
            }
            while (startWatch) {
                val transactionResult = channnel.receive()
            }
        }
    }

    fun watachBlock(){
        GlobalScope.launch {
            val channel = chain.watchBlock { true }
            while (startWatch) {
                var block = channel.receive()
            }

        }
    }
}