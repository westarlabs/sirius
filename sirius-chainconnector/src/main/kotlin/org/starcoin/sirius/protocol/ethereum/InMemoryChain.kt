package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.runBlocking
import org.ethereum.core.CallTransaction.createRawTransaction
import org.ethereum.solidity.SolidityType
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.EventTopic
import org.starcoin.sirius.protocol.TransactionResult
import java.math.BigInteger

sealed class ChainCtlMessage {
    class NewBlock(val response: SendChannel<EthereumBlock?>) : ChainCtlMessage()
}

class InMemoryChain(val autoGenblock: Boolean = true) : EthereumBaseChain() {

    //StandaloneBlockchain autoblock has concurrent problem
    val sb = StandaloneBlockchain().withAutoblock(false).withGasLimit(500000000)

    val chainAcctor = chainActor()

    fun chainActor() = GlobalScope.actor<ChainCtlMessage> {
        for (msg in channel) {
            when (msg) {
                is ChainCtlMessage.NewBlock -> {
                    try {
                        val block = EthereumBlock(sb.createBlock())
                        msg.response.send(block)
                    } catch (ex: Exception) {
                        msg.response.send(null)
                    }
                }
            }
        }
    }

    override fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (TransactionResult<EthereumTransaction>) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): Channel<TransactionResult<EthereumTransaction>> {
        var transactionChannel = Channel<TransactionResult<EthereumTransaction>>(200)
        //TODO remove listener on channel close
        sb.addEthereumListener(TransactionListener(transactionChannel, filter))
        return transactionChannel
    }

    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBlock(height: BigInteger): EthereumBlock? {
        val blockStore = sb.blockchain.blockStore
        val hash = blockStore.getBlockHashByNumber(height.longValueExact())
        val block = hash?.let { blockStore.getBlockByHash(hash) }
        return block?.let { EthereumBlock(block) }
    }

    override fun watchBlock(filter: (EthereumBlock) -> Boolean): Channel<EthereumBlock> {
        var blockChannel = Channel<EthereumBlock>(200)
        //TODO remove listener on channel close
        sb.addEthereumListener(BlockListener(blockChannel, filter))
        return blockChannel
    }

    override fun getBalance(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getBalance(address.toBytes())
    }

    override fun getNonce(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getNonce(address.toBytes())
    }

    override fun findTransaction(hash: Hash): EthereumTransaction? {
        val tx = sb.blockchain.transactionStore.get(hash.toBytes())?.firstOrNull()
        return tx?.let { EthereumTransaction(tx.receipt.transaction) }
    }

    override fun submitTransaction(account: EthereumAccount, transaction: EthereumTransaction): Hash = runBlocking {
        val key = account.key as EthCryptoKey
        sb.sender = key.ecKey
        transaction.sign(key)
        sb.submitTransaction(transaction.toEthTransaction())
        account.getAndIncNonce()
        val response = Channel<EthereumBlock?>(1)
        chainAcctor.send(ChainCtlMessage.NewBlock(response))
        //TODO async, not wait block create.
        response.receive()
        transaction.hash()
    }

    val bytesType: SolidityType.BytesType = SolidityType.BytesType()

    override fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray {
        val tx = createRawTransaction(0, 0, 100000000000000L, contractAddress.toBytes().toHEXString(), 0, data)
        tx.sign((caller as EthCryptoKey).ecKey)
        val callBlock = sb.blockchain.bestBlock
        val repository = this.sb.blockchain.getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking()

        try {
            val executor = org.ethereum.core.TransactionExecutor(
                tx, callBlock.getCoinbase(), repository, sb.blockchain.getBlockStore(),
                sb.blockchain.getProgramInvokeFactory(), callBlock
            )
                .setLocalCall(true)

            executor.init()
            executor.execute()
            executor.go()
            executor.finalization()
            if (executor.result.isRevert || executor.result.exception != null) {
                //TODO define custom error.
                throw RuntimeException("callConstFunction fail")
            }
            val bytes = executor.result.hReturn
            return bytesType.decode(bytes, SolidityType.IntType.decodeInt(bytes, 0).toInt()) as ByteArray
        } finally {
            repository.rollback()
        }
    }

    override fun getBlockNumber(): Long {
        return sb.blockchain.bestBlock.number
    }

    override fun newTransaction(account: EthereumAccount,to:Address,value:BigInteger):EthereumTransaction {
        var ethereumTransaction = EthereumTransaction(
            to, account.getNonce(), 21000.toBigInteger(),
            210000.toBigInteger(), value
        )
        return ethereumTransaction
    }

    fun createBlock(): EthereumBlock = runBlocking {
        val response = Channel<EthereumBlock?>(1)
        chainAcctor.send(ChainCtlMessage.NewBlock(response))
        response.receive() ?: throw RuntimeException("Create Block fail.")
    }

    fun miningCoin(address: Address, amount: BigInteger) {
        this.sb.sendEther(address.toBytes(), amount)
        this.createBlock()
    }
}
