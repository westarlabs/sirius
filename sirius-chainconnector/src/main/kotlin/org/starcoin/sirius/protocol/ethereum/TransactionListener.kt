package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.ethereum.core.BlockSummary
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.math.BigInteger
import java.nio.charset.Charset

class TransactionListener(
    val transactionChannel: Channel<TransactionResult<EthereumTransaction>>,
    var transactionFilter: (TransactionResult<EthereumTransaction>) -> Boolean
) : AbstractEthereumListener() {

    companion object : WithLogging()

    override fun onBlock(blockSummary: BlockSummary) {
        GlobalScope.launch {
            blockSummary.block.transactionsList.forEachIndexed { index, it ->
                var ethereumTransaction = EthereumTransaction(it)
                val txReceipt = blockSummary.receipts[index]
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
                        txReceipt.isTxStatusOK
                    )
                )
                LOG.info("TransactionListener tx:${ethereumTransaction.hash()}")
                if (txReceipt.error != null && txReceipt.error.isNotEmpty()) {
                    LOG.warning("tx ${ethereumTransaction.hash()} error: ${txReceipt.error}")
                }
                for (log in txReceipt.logInfoList) {
                    LOG.fine("tx ${ethereumTransaction.hash()} log $log")
                }
                if (!txReceipt.isSuccessful || !txReceipt.isTxStatusOK) {
                    val file = File.createTempFile("trace", ".txt")
                    val trace = traceMap[ethereumTransaction.hash()]
                    file.printWriter().use { out -> out.println(trace) }
                    LOG.warning("Write tx trace file to ${file.absolutePath}")
                    val parser = JSONParser()
                    val jsonObject = parser.parse(trace) as JSONObject
                    val result = (jsonObject["result"] as String).hexToByteArray().toString(Charset.defaultCharset())
                    LOG.warning("tx ${ethereumTransaction.hash()} trace result $result")
                }
                if (transactionFilter(transactionResult)) {
                    transactionChannel.send(transactionResult)
                }
            }
        }
    }
}
