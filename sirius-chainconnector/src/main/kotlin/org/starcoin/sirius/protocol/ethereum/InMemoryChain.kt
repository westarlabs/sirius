package org.starcoin.sirius.protocol.ethereum

import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.protocol.Chain
import java.math.BigInteger

class InMemoryChain(autoGenblock :Boolean):Chain {

    val autoGenblock = autoGenblock
    val sb = StandaloneBlockchain()
    val inMemoryEthereumListener = InMemoryEthereumListener()

    override fun getBlock(height: BigInteger): BlockInfo? {
        return inMemoryEthereumListener.blocks.get(height.toInt())
    }

    override fun watchBlock(onNext: (block: BlockInfo) -> Unit) {
        sb.addEthereumListener(inMemoryEthereumListener)
        if(autoGenblock){
            sb.withAutoblock(autoGenblock)
        }
    }

    override fun watchTransaction(onNext: (tx: ChainTransaction) -> Unit) {

    }

    override fun getBalance(address: BlockAddress): BigInteger {
        return sb.getBlockchain().getRepository().getBalance(address.address)
    }

    override fun findTransaction(hash: Hash): ChainTransaction? {
        return inMemoryEthereumListener.findTransaction(hash)
    }


}