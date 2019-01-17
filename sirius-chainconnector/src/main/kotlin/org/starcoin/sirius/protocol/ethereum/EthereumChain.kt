package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import org.ethereum.crypto.HashUtil
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.EventTopic
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.Utils
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
import org.web3j.utils.Numeric
import java.math.BigInteger


const val DEFAULT_URL = "http://127.0.0.1:8545"
const val GAS_LIMIT_BOUND_DIVISOR = 1024
const val blockGasIncreasePercent = 0

class EthereumChain constructor(httpUrl: String = DEFAULT_URL, socketPath: String? = null) :
    EthereumBaseChain() {

    override fun getBlockNumber(): BigInteger {
        val resp = web3.ethBlockNumber().sendAsync().get()
        if (resp.hasError()) throw RuntimeException(resp.error.message)
        return resp.blockNumber
    }

    val web3: Web3j =
        Web3j.build(if (socketPath != null) UnixIpcService(socketPath) else HttpService(httpUrl))

    override fun getNonce(address: Address): BigInteger {
        //TODO use transactionCount is right?
        return web3.ethGetTransactionCount(
            Numeric.toHexString(address.toBytes()),
            DefaultBlockParameterName.LATEST
        ).send().transactionCount
    }

    override fun submitTransaction(account: EthereumAccount, transaction: EthereumTransaction): Hash {
        transaction.tx.sign((account.key as EthCryptoKey).ecKey)
        val hexTx = Numeric.toHexString(transaction.tx.encoded)
        val resp = web3.ethSendRawTransaction(hexTx).sendAsync().get()
        if (resp.hasError()) throw NewTxException(resp.error)
        account.getAndIncNonce()
        return resp.transactionHash.toHash()
    }

    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): Channel<TransactionResult<EthereumTransaction>> {
        val ch = Channel<TransactionResult<EthereumTransaction>>(10)
        val hx = ArrayList<Hash>(1)
        GlobalScope.launch {
            web3.transactionFlowable().subscribe {
                val txr = TransactionResult(
                    EthereumTransaction(it), getTransactionReceipts(hx)[0]
                )
                if (filter(txr)) ch.sendBlocking(txr)
            }
        }
        return ch
    }

    override
    fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (TransactionResult<EthereumTransaction>) -> Boolean
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
        GlobalScope.launch {
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
                if (filter(txr)) ch.send(txr)
            }
        }
        return ch
    }

    override fun watchBlock(filter: (EthereumBlock) -> Boolean): Channel<EthereumBlock> {
        val ch = Channel<EthereumBlock>(10)
        GlobalScope.launch {
            web3.blockFlowable(true).subscribe {
                it.block.blockInfo()
                if (filter(it.block.blockInfo()))
                    ch.sendBlocking(EthereumBlock(it.block.number.longValueExact(), it.block.hash.toHash()))
            }
        }
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

    override fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray {
        val resp = this.web3.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                caller.address.toString(),
                contractAddress.toString(),
                Utils.HEX.encode(data)
            ), DefaultBlockParameterName.LATEST
        ).sendAsync().get()
        if (resp.hasError()) throw RuntimeException(resp.error.message)
        return resp.value.hexToByteArray()
    }

    private fun Transaction.chainTransaction() = EthereumTransaction(this)

    private fun EthBlock.Block.blockInfo(): EthereumBlock {
        return EthereumBlock(this)
    }

    fun caculateGasLimit(): BigInteger {
        return BigInteger.valueOf(10000000000000)
        /**
        return ByteUtil.bytesToBigInteger(parent.getGasLimit())
        .multiply(GAS_LIMIT_BOUND_DIVISOR * 100 + blockGasIncreasePercent)
        .divide(BigInteger.valueOf((GAS_LIMIT_BOUND_DIVISOR * 100).toLong()))**/
    }

    override fun newTransaction(account: EthereumAccount, to: Address, value: BigInteger): EthereumTransaction {
        return EthereumTransaction(
            to,
            account.getNonce(),
            EthereumBaseChain.defaultGasPrice,
            EthereumBaseChain.defaultGasLimit,
            value
        )
    }

    class NewTxException(error: Error) : Exception(error.message)
    open class WatchTxExecption(error: Error) : Exception(error.message)
    class NewFilterException(error: Error) : WatchTxExecption(error)
    class FilterChangeException(error: Error) : WatchTxExecption(error)
    class GetRecepitException(error: Error) : WatchTxExecption(error)
    class GetTransactionException(error: Error) : WatchTxExecption(error)
}
