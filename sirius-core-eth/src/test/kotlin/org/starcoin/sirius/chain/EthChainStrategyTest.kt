package org.starcoin.sirius.chain

import org.junit.Assert
import org.starcoin.sirius.chain.eth.EthChainStrategy

class EthChainStrategyTest : ChainStrategyTestBase() {
    override fun assertChainStrategyType(strategy: ChainStrategy) {
        Assert.assertTrue(strategy is EthChainStrategy)
    }
}
