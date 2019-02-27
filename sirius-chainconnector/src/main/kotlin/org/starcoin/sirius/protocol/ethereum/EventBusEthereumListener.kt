package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.ethereum.core.BlockSummary
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.starcoin.sirius.channel.EventBus
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.nio.charset.Charset

class EventBusEthereumListener() : AbstractEthereumListener() {

    companion object : WithLogging()

    private val blockEventBus = EventBus<EthereumBlock>()
    private val txEventBus = EventBus<TransactionResult<EthereumTransaction>>()

    override fun onBlock(blockSummary: BlockSummary) = runBlocking {
        val block = EthereumBlock(blockSummary.block, blockSummary.receipts)
        LOG.info("EventBusEthereumListener onBlock hash:${block.hash()}, height:${block.height}, txs:${block.transactions.size}")
        blockEventBus.send(block)
        block.transactions.forEach {
            val tx = it.tx
            val txReceipt = it.receipt as EthereumReceipt
            if (!it.receipt.status) {
                LOG.warning("tx ${tx.hash()} isTxStatusOK: ${txReceipt.txStatus} returnEvent: ${txReceipt.returnEvent}")
                val trace = traceMap[tx.hash()]
                if (trace != null) {
                    val file = File.createTempFile("trace", ".txt")
                    file.printWriter().use { out -> out.println(trace) }
                    LOG.warning("Write tx trace file to ${file.absolutePath}")
                    val parser = JSONParser()
                    val jsonObject = parser.parse(trace) as JSONObject
                    val result =
                        (jsonObject["result"] as String).hexToByteArray().toString(Charset.defaultCharset())
                    LOG.warning("tx ${tx.hash()} trace result $result")
                }
            }
            txEventBus.send(it)
        }
    }

    fun subscribeBlock(filter: (EthereumBlock) -> Boolean): ReceiveChannel<EthereumBlock> {
        return this.blockEventBus.subscribe(filter)
    }

    fun subscribeTx(filter: (TransactionResult<EthereumTransaction>) -> Boolean = { true }): ReceiveChannel<TransactionResult<EthereumTransaction>> {
        return this.txEventBus.subscribe(filter)
    }

    fun close() {
        this.blockEventBus.close()
        this.txEventBus.close()
    }
}
