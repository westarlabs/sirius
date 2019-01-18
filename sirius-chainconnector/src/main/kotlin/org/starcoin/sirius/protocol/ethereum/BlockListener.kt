package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.ethereum.core.BlockSummary
import org.starcoin.sirius.util.WithLogging

class BlockListener(val blockChannel: Channel<EthereumBlock>, val filter: (EthereumBlock) -> Boolean) :
    AbstractEthereumListener() {

    companion object : WithLogging()

    override fun onBlock(blockSummary: BlockSummary) {
        GlobalScope.launch {
            val block = EthereumBlock(blockSummary.block)
            LOG.info("BlockListener onBlock hash:${block.hash}, height:${block.height}, txs:${block.transactions.size}")
            if (filter(block)) {
                blockChannel.send(block)
            }
        }
    }
}
