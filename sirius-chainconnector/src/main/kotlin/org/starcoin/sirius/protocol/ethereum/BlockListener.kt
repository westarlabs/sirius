package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.ethereum.core.BlockSummary
import org.starcoin.sirius.util.WithLogging

//TODO support filter
class BlockListener(val blockChannel: Channel<EthereumBlock>, val filter: (EthereumBlock) -> Boolean) :
    AbstractEthereumListener() {

    companion object : WithLogging()

    override fun onBlock(blockSummary: BlockSummary) {
        GlobalScope.launch {
            val block = EthereumBlock(blockSummary.block)
            if (filter(block)) {
                blockChannel.send(block)
            }
        }
    }
}
