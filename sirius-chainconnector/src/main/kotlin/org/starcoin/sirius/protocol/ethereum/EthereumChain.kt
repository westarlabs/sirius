package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.ChainEvent
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.protocol.TxDeferred
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Response.Error
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthLog.LogObject
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.protocol.websocket.WebSocketService
import java.io.IOException
import java.math.BigInteger


const val DEFAULT_WS = "ws://127.0.0.1:8546"
const val GAS_LIMIT_BOUND_DIVISOR = 1024
const val blockGasIncreasePercent = 0

class EthereumChain constructor(httpUrl: String? = null, socketPath: String? = null, webSocket: String = DEFAULT_WS) :
    EthereumBaseChain() {

    init {
        //TODO destroy job
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val hashList = txDeferreds.keys.toList()
                if (hashList.isNotEmpty()) {
                    val receipts = getTransactionReceipts(hashList)
                    receipts.forEach { receipt ->
                        receipt?.let { completeDeferred(it) }
                    }
                }
                delay(2000)
            }
        }
    }

    override fun waitTransactionProcessed(hash: Hash): TxDeferred {
        return this.registerDeferred(hash)
    }

    override fun waitBlocks(blockCount: Int) {
        val currentBlockNum = getBlockNumber()
        while (currentBlockNum + blockCount >= getBlockNumber()) {
            Thread.sleep(1000)
        }
    }

    override fun getBlockNumber(): Long {
        val resp = web3.ethBlockNumber().sendAsync().get()
        if (resp.hasError()) throw RuntimeException(resp.error.message)
        return resp.blockNumber.longValueExact()

    }

    val web3: Web3j =
        Web3j.build(
            when {
                socketPath != null -> UnixIpcService(socketPath)
                httpUrl != null -> HttpService(httpUrl)
                else -> WebSocketService(webSocket, false).also { it.connect() } //TODO: close connection
            }
        )

    override fun getNonce(address: Address): BigInteger {
        //TODO use transactionCount is right?
        return web3.ethGetTransactionCount(
            address.toString(),
            DefaultBlockParameterName.PENDING
        ).send().transactionCount
    }

    override fun doSubmitTransaction(account: EthereumAccount, transaction: EthereumTransaction): TxDeferred {
        transaction.sign(account.key as EthCryptoKey)
        val hexTx = transaction.toHEXString()
        val resp = web3.ethSendRawTransaction(hexTx).sendAsync().get()
        if (resp.hasError()) throw NewTxException(resp.error)
        account.incAndGetNonce()
        return registerDeferred(resp.transactionHash.toHash())
    }

    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): ReceiveChannel<TransactionResult<EthereumTransaction>> {
        val ch = Channel<TransactionResult<EthereumTransaction>>()
        GlobalScope.launch(Dispatchers.IO) {
            web3.transactionFlowable().subscribe {
                val receipts = getTransactionReceipts(listOf(Hash.wrap(it.hash)))
                val txr = TransactionResult(
                    EthereumTransaction(it), receipts[0]!!
                )
                if (filter(txr)) ch.sendBlocking(txr)
            }
        }
        return ch
    }

    override
    fun watchEvents(
        contract: Address,
        events: Collection<ChainEvent>,
        filter: (TransactionResult<EthereumTransaction>) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        val ch = Channel<TransactionResult<EthereumTransaction>>(1)
        val ethFilter = EthFilter(
            DefaultBlockParameterName.EARLIEST,
            DefaultBlockParameterName.LATEST,
            contract.toString()
        )
        events.forEach { ethFilter.addSingleTopic(org.web3j.crypto.Hash.sha3String(it.eventSignature)) }
        val newFilterResp = web3.ethNewFilter(ethFilter).sendAsync().get()
        if (newFilterResp.hasError()) throw NewFilterException(newFilterResp.error)
        GlobalScope.launch(Dispatchers.IO) {
            var filterChangeResp = web3.ethGetFilterChanges(newFilterResp.filterId).sendAsync().get()
            while (filterChangeResp.logs.size == 0) {
                filterChangeResp = web3.ethGetFilterChanges(newFilterResp.filterId).sendAsync().get()
                if (filterChangeResp.hasError()) throw FilterChangeException(filterChangeResp.error)
                delay(1000)
            }
            filterChangeResp.logs.map { it as LogObject }.forEach {
                val recepitResp = web3.ethGetTransactionReceipt(it.transactionHash).sendAsync().get()
                if (recepitResp.hasError()) throw GetRecepitException(recepitResp.error)
                val r = recepitResp.transactionReceipt.get()
                val receipt = EthereumReceipt(r)
                val txResp = web3.ethGetTransactionByHash(r.transactionHash).sendAsync().get()
                if (txResp.hasError()) throw GetTransactionException(txResp.error)
                val tx = txResp.transaction.get().chainTransaction()
                val txr = TransactionResult(tx, receipt)
                if (filter(txr)) ch.send(txr)
            }
        }
        return ch
    }

    override fun watchBlock(
        startBlockNum: BigInteger,
        filter: (EthereumBlock) -> Boolean
        ): Channel<EthereumBlock> {
        val ch = Channel<EthereumBlock>()
        GlobalScope.launch {
            val headNotify = web3.newHeadsNotifications()
            var syncBlockNum = BigInteger.valueOf(-1)
            if (startBlockNum != BigInteger.valueOf(-1)) {
                syncBlockNum = getBlockNumber().toBigInteger()
            }
            var height = startBlockNum
            while (height <= syncBlockNum) {
                val block = getBlock(height)!!
                if (filter(block)) ch.sendBlocking(block)
                height = height.inc()
            }
            val notifych = Channel<BigInteger>()
            GlobalScope.launch(Dispatchers.IO) {
                headNotify.subscribe(
                    {
                        val blockNum = it.params.result.number.hexToByteArray().toBigInteger()
                        if (blockNum > syncBlockNum) {
                            notifych.sendBlocking(blockNum)
                        }
                    },
                    {},
                    {
                        notifych.close()
                    })
            }
            GlobalScope.launch(Dispatchers.IO) {
                while (!notifych.isClosedForSend) {
                    val block = getBlock(notifych.receive())!!
                    ch.sendBlocking(block)
                }
            }
        }
        return ch
    }

    override fun getBalance(address: Address): BigInteger {
        val req =
            web3.ethGetBalance(address.toString(), DefaultBlockParameterName.LATEST).sendAsync()
                .get()
        if (req.hasError()) throw IOException(req.error.message)

        return req.balance
    }

    override fun findTransaction(hash: Hash): EthereumTransaction? {
        val resp = web3.ethGetTransactionByHash(hash.toString())
            .send()
        if (resp.hasError()) throw Exception(resp.error.message)
        val tx = resp.transaction.orElse(null)
        return tx.chainTransaction()
    }

    override fun getBlock(height: BigInteger): EthereumBlock? {
        val blockReq = web3.ethGetBlockByNumber(
            if (height == BigInteger.valueOf(-1)) DefaultBlockParameterName.LATEST else
                DefaultBlockParameter.valueOf(height),
            true
        ).sendAsync().get()
        if (blockReq.hasError()) throw IOException(blockReq.error.message)
        return blockReq.block.blockInfo()
    }

    private fun doGetTransactionReceipts(block: EthBlock.Block): List<Receipt> {
        return this.doGetTransactionReceipts(block.transactions.map { val tx = it as Transaction; tx.hash })
            .map { it!! }
    }

    private fun doGetTransactionReceipts(txHashs: List<String>): List<Receipt?> {
        return txHashs.map {
            //send Async batch, then get result.
            web3.ethGetTransactionReceipt(it).sendAsync()
        }.map {
            val recepitResp = it.get()
            //TODO not find resp .
            if (recepitResp.hasError()) throw GetRecepitException(recepitResp.error)
            recepitResp.transactionReceipt.orElse(null)?.let { EthereumReceipt(it) }
        }
    }

    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt?> {
        return this.doGetTransactionReceipts(txHashs.map { it.toString() })
    }


    override fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray {
        val resp = this.web3.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                caller.address.toString(),
                contractAddress.toString(),
                data.toHEXString()
            ), DefaultBlockParameterName.LATEST //TODO: Pass the blockNum in receipt better?
        ).sendAsync().get()
        if (resp.hasError()) throw RuntimeException(resp.error.message)
        return resp.value.hexToByteArray()
    }

    private fun Transaction.chainTransaction() = EthereumTransaction(this)

    private fun EthBlock.Block.blockInfo(): EthereumBlock {
        return EthereumBlock(this, doGetTransactionReceipts(this))
    }

    fun caculateGasLimit(): BigInteger {
        return BigInteger.valueOf(10000000000000)
        /**
        return ByteUtil.bytesToBigInteger(parent.getGasLimit())
        .multiply(GAS_LIMIT_BOUND_DIVISOR * 100 + blockGasIncreasePercent)
        .divide(BigInteger.valueOf((GAS_LIMIT_BOUND_DIVISOR * 100).toLong()))**/
    }

    class NewTxException(error: Error) : Exception(error.message)
    open class WatchTxExecption(error: Error) : Exception(error.message)
    class NewFilterException(error: Error) : WatchTxExecption(error)
    class FilterChangeException(error: Error) : WatchTxExecption(error)
    class GetRecepitException(error: Error) : WatchTxExecption(error)
    class GetTransactionException(error: Error) : WatchTxExecption(error)

    override fun newTransaction(account: EthereumAccount, to: Address, value: BigInteger): EthereumTransaction {
        return EthereumTransaction(
            to,
            account.getNonce(),
            EthereumBaseChain.defaultGasPrice,
            EthereumBaseChain.defaultGasLimit,
            value
        )
    }
}
