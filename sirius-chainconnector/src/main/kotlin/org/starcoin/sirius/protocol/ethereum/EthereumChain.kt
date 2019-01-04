package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import org.ethereum.crypto.HashUtil
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Response.Error
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthLog.LogObject
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.utils.Numeric
import java.lang.Exception
import java.math.BigInteger

const val defaultHttpUrl = "http://127.0.0.1:8545"


class EthereumChain constructor(httpUrl: String = defaultHttpUrl, socketPath: String? = null) :
    Chain<EthereumTransaction, EthereumBlock, HubContract> {


    val web3: Web3j =
        Web3j.build(if (socketPath != null) UnixIpcService(socketPath) else HttpService(httpUrl))


    fun getNonce(address: Address): Long {
        return web3.ethGetTransactionCount(
            Numeric.toHexString(address.toBytes()),
            DefaultBlockParameterName.LATEST
        ).send().transactionCount.toLong()
    }

    override fun newTransaction(key: CryptoKey, transaction: EthereumTransaction) {
        transaction.ethTx.sign((key as EthCryptoKey).ecKey)
        val hexTx = Numeric.toHexString(transaction.ethTx.encoded)
        val resp = web3.ethSendRawTransaction(hexTx).sendAsync().get()
        if (resp.hasError()) throw NewTxException(resp.error)
    }

    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override
    fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        val ch = Channel<TransactionResult<EthereumTransaction>>(10)
        val ethFilter = EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            Numeric.toHexString(contract.toBytes())
        )
        val topicHex = Numeric.toHexString(HashUtil.sha256(topic.name.toByteArray()))
        ethFilter.addSingleTopic(topicHex)
        val newFilterResp = web3.ethNewFilter(ethFilter).sendAsync().get()
        if (newFilterResp.hasError()) throw NewFilterException(newFilterResp.error)
        val filterChangeResp = web3.ethGetFilterChanges(newFilterResp.filterId).sendAsync().get()
        if (filterChangeResp.hasError()) throw FilterChangeException(filterChangeResp.error)
        filterChangeResp.logs.map { it as LogObject }.forEach {
            val recepitResp = web3.ethGetTransactionReceipt(it.transactionHash).sendAsync().get()
            if (recepitResp.hasError()) throw GetRecepitException(recepitResp.error)
            val r = recepitResp.transactionReceipt.get()
            val receipt = Receipt(
                r.transactionHash, r.transactionIndex,
                r.blockHash, r.blockNumber, r.contractAddress,
                r.from, r.to, r.gasUsed, r.logsBloom,
                r.cumulativeGasUsed, r.root, r.isStatusOK
            )
            val txResp = web3.ethGetTransactionByHash(r.transactionHash).sendAsync().get()
            if (txResp.hasError()) throw GetTransactionException(txResp.error)
            val tx = txResp.transaction.get().chainTransaction()
            val txr = TransactionResult(tx, receipt)
            GlobalScope.launch {
                ch.send(txr)
            }
        }
        return ch
    }

    override fun watchBlock(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean
    ): Channel<EthereumBlock> {
        val ch = Channel<EthereumBlock>(10)
        web3.blockFlowable(true).subscribe { block -> block.block.blockInfo() }
        return ch

    }

    override fun getBalance(address: Address): BigInteger {
        val req =
            web3.ethGetBalance(Numeric.toHexString(address.toBytes()), DefaultBlockParameterName.LATEST).sendAsync()
                .get()
        if (req.hasError()) throw IOException(req.error.message)

        return req.balance
    }


    override fun findTransaction(hash: Hash): EthereumTransaction? {
        val resp = web3.ethGetTransactionByHash(hash.toString()).send()
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

        // FIXME: Use BigInbteger in blockinfo
        val blockInfo = EthereumBlock(blockReq.block)
        blockReq.block.transactions.map { it ->
            val tx = it as Transaction
            blockInfo.addTransaction(tx.chainTransaction())
        }
        return blockInfo
    }

    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt> {
        return txHashs.map {
            val recepitResp = web3.ethGetTransactionReceipt(it.toString()).sendAsync().get()
            if (recepitResp.hasError()) throw GetRecepitException(recepitResp.error)
            val r = recepitResp.transactionReceipt.get()
            Receipt(
                r.transactionHash, r.transactionIndex,
                r.blockHash, r.blockNumber, r.contractAddress,
                r.from, r.to, r.gasUsed, r.logsBloom,
                r.cumulativeGasUsed, r.root, r.isStatusOK
            )
        }
    }

    override fun getContract(parameter: QueryContractParameter): HubContract {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun Transaction.chainTransaction(): EthereumTransaction {
        return EthereumTransaction(
            Address.wrap(this.to),
            System.currentTimeMillis(),  //timestamp
            this.gasPrice.longValueExact(),
            0,
            this.value.toLong(),
            this.input.toByteArray()
        )
    }

    private fun EthBlock.Block.blockInfo(): EthereumBlock {
        return EthereumBlock(this)
    }

    class NewTxException(error: Error) : Exception(error.message)
    open class WatchTxExecption(error: Error) : Exception(error.message)
    class NewFilterException(error: Error) : WatchTxExecption(error)
    class FilterChangeException(error: Error) : WatchTxExecption(error)
    class GetRecepitException(error: Error) : WatchTxExecption(error)
    class GetTransactionException(error: Error) : WatchTxExecption(error)
}
