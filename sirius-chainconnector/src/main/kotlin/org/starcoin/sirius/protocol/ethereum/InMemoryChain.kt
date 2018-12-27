package org.starcoin.sirius.protocol.ethereum

import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.*
import java.math.BigInteger

class InMemoryChain(autoGenblock: Boolean) : Chain<EthereumTransaction, EthereumBlock, HubContract> {

    private val autoGenblock = autoGenblock
    val sb = StandaloneBlockchain().withAutoblock(autoGenblock)
    private val inMemoryEthereumListener = InMemoryEthereumListener()

    override fun getBlock(height: BigInteger): EthereumBlock? {
        return inMemoryEthereumListener.blocks.get(height.toInt())
    }

    override fun watchBlock(onNext: (block: EthereumBlock) -> Unit) {
        sb.addEthereumListener(inMemoryEthereumListener)
        if(autoGenblock){
            sb.withAutoblock(autoGenblock)
        }
    }

    override fun watchTransactions(onNext: (tx: EthereumTransaction) -> Unit) {

    }

    override fun getBalance(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getBalance(address.toBytes())
    }

    override fun findTransaction(hash: Hash): EthereumTransaction? {
        return inMemoryEthereumListener.findTransaction(hash)
    }

    override fun newTransaction(key: CryptoKey, transaction: EthereumTransaction) {
        transaction.ethTx.sign((key as EthCryptoKey).ecKey)
        sb.submitTransaction(transaction.ethTx)
    }

    override fun watchTransaction(txHash: Hash, listener: TransactionProgressListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContract(parameter: QueryContractParameter): HubContract {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
