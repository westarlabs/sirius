package org.starcoin.sirius.hub

import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction

interface HubChainConnection {

    fun watchBlock(block: (BlockInfo) -> Unit)

    fun submitTransaction(transaction: ChainTransaction)

}
