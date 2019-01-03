package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.channels.Channel
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.*
import java.math.BigInteger

class InMemoryChain(autoGenblock: Boolean) : Chain<EthereumTransaction, EthereumBlock, HubContract> {
    override fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun watchTransactions(filter: (FilterArguments) -> Boolean): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val autoGenblock = autoGenblock
    val sb = StandaloneBlockchain().withAutoblock(autoGenblock).withGasLimit(500000000)

    private val inMemoryEthereumListener = InMemoryEthereumListener()

    override fun getBlock(height: BigInteger): EthereumBlock? {
        return inMemoryEthereumListener.blocks.get(height.toInt())
    }

    override fun watchBlock(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean
    ) :Channel<EthereumBlock>{
        var blockChannel = Channel<EthereumBlock>()
        inMemoryEthereumListener.blockChannel = blockChannel
        sb.addEthereumListener(inMemoryEthereumListener)
        if(autoGenblock){
            sb.withAutoblock(autoGenblock)
        }
        return blockChannel
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

    override fun getContract(parameter: QueryContractParameter): HubContract {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
