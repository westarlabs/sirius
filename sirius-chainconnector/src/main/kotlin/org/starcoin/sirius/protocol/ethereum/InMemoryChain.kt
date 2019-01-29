package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.ethereum.core.CallTransaction.createRawTransaction
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.toNoPrefixHEXString
import org.starcoin.sirius.protocol.ChainEvent
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import java.math.BigInteger

sealed class ChainCtlMessage {
    class NewTransaction(val tx: EthereumTransaction, val response: SendChannel<EthereumBlock?>) : ChainCtlMessage()
    class NewBlock(val response: SendChannel<EthereumBlock?>) : ChainCtlMessage()
}

class InMemoryChain(val autoGenblock: Boolean = true) : EthereumBaseChain() {
    
    override fun waitTransactionProcessed(hash: Hash, times: Int) {
    }

    override fun waitBlocks(blockCount: Int) {
        for (i in blockCount.downTo(0))
            sb.createBlock()
    }

    //StandaloneBlockchain autoblock has concurrent problem
    val sb = StandaloneBlockchain().withAutoblock(false).withGasLimit(500000000)
    val originAccount = sb.sender!!
    val eventBus = EventBusEthereumListener()

    init {
        sb.addEthereumListener(eventBus)
    }

    val chainAcctor =
        GlobalScope.actor<ChainCtlMessage>(start = CoroutineStart.LAZY, onCompletion = {
            it?.let { ex -> LOG.warning("chain actor onCompletion ${ex.message}") }
                ?: LOG.info("chain actor onCompletion")
        }) {
            consumeEach { msg ->
                when (msg) {
                    is ChainCtlMessage.NewBlock -> msg.response.send(doCreateBlock())
                    is ChainCtlMessage.NewTransaction -> {
                        sb.submitTransaction(msg.tx.toEthTransaction())
                        if (autoGenblock) {
                            msg.response.send(doCreateBlock())
                        } else {
                            msg.response.send(null)
                        }
                    }
                }
            }
        }

    private fun doCreateBlock(): EthereumBlock? {
        return try {
            val block = EthereumBlock(sb.createBlock())
            LOG.info("InMemoryChain create NewBlock: ${block.hash}, txs: ${block.transactions.size}")
            block
        } catch (ex: Exception) {
            ex.printStackTrace()
            LOG.severe(ex.message)
            null
        }
    }

    override fun watchEvents(
        contract: Address,
        events: Collection<ChainEvent>,
        filter: (TransactionResult<EthereumTransaction>) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): ReceiveChannel<TransactionResult<EthereumTransaction>> {
        return eventBus.subscribeTx(filter)
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

    override fun watchBlock(filter: (EthereumBlock) -> Boolean): ReceiveChannel<EthereumBlock> {
        return eventBus.subscribeBlock(filter)
    }

    override fun getBalance(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getBalance(address.toBytes())
    }

    override fun getNonce(address: Address): BigInteger {
        return sb.blockchain.repository.getNonce(address.toBytes())
    }

    override fun findTransaction(hash: Hash): EthereumTransaction? {
        val tx = sb.blockchain.transactionStore.get(hash.toBytes())?.firstOrNull()
        return tx?.let { EthereumTransaction(tx.receipt.transaction) }
    }

    override fun submitTransaction(account: EthereumAccount, transaction: EthereumTransaction): Hash {
        val key = account.key as EthCryptoKey
        sb.sender = key.ecKey
        transaction.sign(key)
        val chainNonce = sb.blockchain.repository.getNonce(account.address.toBytes())
        LOG.fine("chainNonce ${account.address} $chainNonce")
        LOG.fine("${account.address} submitTransaction hash:${transaction.hash()} nonce:${transaction.nonce} contractFunction:${transaction.contractFunction} dataSize:${transaction.data?.size}")
        account.incAndGetNonce()
        val response = Channel<EthereumBlock?>(1)
        GlobalScope.launch(Dispatchers.IO) {
            chainAcctor.send(ChainCtlMessage.NewTransaction(transaction, response))
        }
        //TODO async, not wait block create.
        runBlocking {
            val block = response.receive()
            block?.let { LOG.fine("submitTransaction receive block ${block.hash}") }
                ?: LOG.fine("submitTransaction receive block null.")
        }
        return transaction.hash()
    }

    override fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray {
        val tx = createRawTransaction(0, 0, 100000000000000L, contractAddress.toBytes().toNoPrefixHEXString(), 0, data)
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
            return executor.result.hReturn
        } finally {
            repository.rollback()
        }
    }

    override fun getBlockNumber(): Long {
        return sb.blockchain.bestBlock.number
    }

    override fun newTransaction(account: EthereumAccount, to: Address, value: BigInteger): EthereumTransaction {
        return EthereumTransaction(
            to, account.getNonce(), defaultGasPrice,
            defaultGasLimit, value
        )
    }

    fun createBlock(): EthereumBlock = runBlocking {
        val response = Channel<EthereumBlock?>(1)
        chainAcctor.send(ChainCtlMessage.NewBlock(response))
        response.receive() ?: throw RuntimeException("Create Block fail.")
    }

    fun miningCoin(account: EthereumAccount, amount: BigInteger) {
        this.sb.sender = originAccount
        this.sb.sendEther(account.address.toBytes(), amount)
        this.createBlock()
    }

    override fun stop() {
        this.chainAcctor.close()
        this.eventBus.close()
    }
}
