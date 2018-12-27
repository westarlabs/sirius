package org.starcoin.sirius.hub

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction

interface HubChainConnection {

    fun watchBlock(block: (Block<*>) -> Unit)

    fun submitTransaction(transaction: ChainTransaction)

}
