package org.starcoin.sirius.chain

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.core.OffchainTransaction
import org.starcoin.sirius.protocol.CommitFunction

abstract class ChainStrategyTestBase {

    @Before
    fun setup() {
        val strategy = ChainStrategy.instance
        println("ChainStrategy: ${strategy.javaClass.name}")
        assertChainStrategyType(strategy)
    }

    abstract fun assertChainStrategyType(strategy: ChainStrategy)

    @Test
    fun testEncodeAndDecode() {
        val tx = OffchainTransaction.mock()
        val bytes = ChainStrategy.encode(tx)
        val tx1 = ChainStrategy.decode(bytes, OffchainTransaction::class)
        Assert.assertEquals(tx, tx1)
        val bytes1 = ChainStrategy.encode(tx1)
        Assert.assertArrayEquals(bytes, bytes1)
    }

    @Test
    fun testEncodeAndDecodeContractFunction() {
        val hubRoot = HubRoot.mock()
        val bytes = ChainStrategy.encode(CommitFunction, hubRoot)
        val hubRoot1 = ChainStrategy.decode(CommitFunction, bytes)
        Assert.assertEquals(hubRoot, hubRoot1)
    }
}
