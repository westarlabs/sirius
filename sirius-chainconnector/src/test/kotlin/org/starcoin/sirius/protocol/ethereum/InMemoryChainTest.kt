package org.starcoin.sirius.protocol.ethereum

import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates

class InMemoryChainTest {

    var chain: InMemoryChain by Delegates.notNull()

    @Before
    fun before() {
        chain = InMemoryChain(true)
    }

    @Test
    fun testMineAndTransfer() {
        val account1 = EthereumAccount()
        val account2 = EthereumAccount()
        val oneEther = EtherUtil.convert(1, EtherUtil.Unit.ETHER)
        chain.miningCoin(account1, oneEther * 1000.toBigInteger())
        Assert.assertEquals(chain.getBalance(account1.address), oneEther * 1000.toBigInteger())

        chain.miningCoin(account2, oneEther * 1000.toBigInteger())
        Assert.assertEquals(chain.getBalance(account2.address), oneEther * 1000.toBigInteger())

        chain.submitTransaction(
            account1,
            chain.newTransaction(account1, account2.address, oneEther * 500.toBigInteger())
        )

        Assert.assertTrue(chain.getBalance(account1.address) > oneEther * 499.toBigInteger())
        Assert.assertEquals(oneEther * 1500.toBigInteger(), chain.getBalance(account2.address))

        chain.miningCoin(account1, oneEther * 10000.toBigInteger())
        Assert.assertTrue(chain.getBalance(account1.address) > oneEther * 10499.toBigInteger())

        Assert.assertEquals(1, account1.getNonce())
        Assert.assertEquals(0, account2.getNonce())

        Assert.assertEquals(account1.getNonce(), chain.getNonce(account1.address).longValueExact())
        Assert.assertEquals(account2.getNonce(), chain.getNonce(account2.address).longValueExact())
    }
}
