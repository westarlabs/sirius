package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.ethereum.core.BlockSummary
import org.ethereum.core.CallTransaction
import org.ethereum.solidity.SolidityType
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.starcoin.sirius.channel.EventBus
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.math.BigInteger
import java.nio.charset.Charset

class EventBusEthereumListener() : AbstractEthereumListener() {

    companion object : WithLogging()

    private val blockEventBus = EventBus<EthereumBlock>()
    private val txEventBus = EventBus<TransactionResult<EthereumTransaction>>()

    //TODO handle event
    private val returnEventFunction = CallTransaction.Function.fromSignature("ReturnEvent", "bool")

    private val boolType = SolidityType.BoolType()

    override fun onBlock(blockSummary: BlockSummary) {
        GlobalScope.launch(Dispatchers.IO) {
            val block = EthereumBlock(blockSummary.block)
            blockEventBus.send(block)
            LOG.info("EventBusEthereumListener onBlock hash:${block.hash}, height:${block.height}, txs:${block.transactions.size}")
            blockSummary.block.transactionsList.forEachIndexed { index, it ->
                val ethereumTransaction = EthereumTransaction(it)
                val txReceipt = blockSummary.receipts[index]
                val executeSummary = blockSummary.summaries[index]

                LOG.info("EventBusEthereumListener tx:${ethereumTransaction.hash()}")
                if (txReceipt.error != null && txReceipt.error.isNotEmpty()) {
                    LOG.warning("tx ${ethereumTransaction.hash()} error: ${txReceipt.error}")
                }
                var returnEvent: Boolean? = null
                for (log in executeSummary.logs) {
                    LOG.fine("tx ${ethereumTransaction.hash()} log $log")
                    if (returnEventFunction.encodeSignatureLong().contentEquals(log.getTopics().get(0).getData())) {
                        returnEvent = boolType.decode(log.data) as Boolean
                        LOG.fine("tx ${ethereumTransaction.hash()} returnEvent: $returnEvent")
                    }
                }
                LOG.info("tx ${ethereumTransaction.hash()}  PostTxState ${txReceipt.postTxState.toHEXString()}")
                val transactionResult = TransactionResult(
                    ethereumTransaction, Receipt(
                        it.hash,
                        BigInteger.valueOf(index.toLong()),
                        blockSummary.block.hash,
                        BigInteger.valueOf(blockSummary.block.number),
                        null,
                        it.sender,
                        it.receiveAddress,
                        BigInteger.valueOf(blockSummary.block.header.gasUsed),
                        blockSummary.block.header.logsBloom.toHEXString(),
                        BigInteger.valueOf(0),
                        blockSummary.block.header.receiptsRoot.toHEXString(),
                        //txReceipt.isTxStatusOK && txReceipt.isSuccessful && (txReceipt.executionResult.isEmpty() || !txReceipt.executionResult.isZeroBytes())
                        txReceipt.isTxStatusOK && txReceipt.isSuccessful && (returnEvent == null || returnEvent)
                    )
                )
                if (!transactionResult.receipt.status) {
                    //some eth implements not save executionResult: ${txReceipt.executionResult.toHEXString()}", so use event to return.
                    LOG.warning("tx ${ethereumTransaction.hash()} isTxStatusOK: ${txReceipt.isTxStatusOK} isSuccessful: ${txReceipt.isSuccessful} returnEvent: $returnEvent")
                    val trace = traceMap[ethereumTransaction.hash()]
                    if (trace != null) {
                        val file = File.createTempFile("trace", ".txt")
                        file.printWriter().use { out -> out.println(trace) }
                        LOG.warning("Write tx trace file to ${file.absolutePath}")
                        val parser = JSONParser()
                        val jsonObject = parser.parse(trace) as JSONObject
                        val result =
                            (jsonObject["result"] as String).hexToByteArray().toString(Charset.defaultCharset())
                        LOG.warning("tx ${ethereumTransaction.hash()} trace result $result")
                    }
                }
                txEventBus.send(transactionResult)
            }
        }
    }

    fun subscribeBlock(filter: (EthereumBlock) -> Boolean): ReceiveChannel<EthereumBlock> {
        return this.blockEventBus.subscribe(filter)
    }

    fun subscribeTx(filter: (TransactionResult<EthereumTransaction>) -> Boolean): ReceiveChannel<TransactionResult<EthereumTransaction>> {
        return this.txEventBus.subscribe(filter)
    }

    fun close() {
        this.blockEventBus.close()
        this.txEventBus.close()
    }
}
