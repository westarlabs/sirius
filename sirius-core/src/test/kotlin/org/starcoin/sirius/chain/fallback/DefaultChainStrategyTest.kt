package org.starcoin.sirius.chain.fallback

import org.junit.Assert
import org.starcoin.sirius.chain.ChainStrategy
import org.starcoin.sirius.chain.ChainStrategyTestBase

class DefaultChainStrategyTest : ChainStrategyTestBase() {
    override fun assertChainStrategyType(strategy: ChainStrategy) {
        Assert.assertTrue(strategy is DefaultChainStrategy)
    }
}
