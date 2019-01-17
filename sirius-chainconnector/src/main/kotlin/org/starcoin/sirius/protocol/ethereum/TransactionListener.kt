package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.ethereum.core.BlockSummary
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

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
                if (transactionFilter(transactionResult)) {
                    transactionChannel.send(transactionResult)
                }
            }
        }
    }
}
