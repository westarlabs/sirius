package org.starcoin.sirius.protocol.ethereum

import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.Chain
import java.math.BigInteger

class InMemoryChain(autoGenblock :Boolean):Chain {

    val autoGenblock = autoGenblock
    val sb = StandaloneBlockchain()
    val inMemoryEthereumListener = InMemoryEthereumListener()

    override fun getBlock(height: BigInteger): BlockInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun watchBlock(onNext: (block: BlockInfo) -> Unit) {
        sb.addEthereumListener(inMemoryEthereumListener)
        if(autoGenblock){
            sb.withAutoblock(autoGenblock)
        }
    }

    override fun watchTransaction(onNext: (tx: ChainTransaction) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBalance(address: ByteArray): BigInteger {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findTransaction(hash: ByteArray): ChainTransaction? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}