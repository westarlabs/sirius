package org.starcoin.sirius.chain

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import java.net.URI

class ChainProviderTest {

    @Test
    fun testInmemory() {
        val connectorString = "chain:inmemory:test"
        val uri = URI(connectorString)
        Assert.assertTrue(ChainProvider.isSupport(uri))
        val chain = ChainProvider.createChain(uri)
        Assert.assertTrue(chain is InMemoryChain)
    }

    @Test
    fun testEthereumChain() {
        val connectorString = "chain:ethereum:http://127.0.0.1:2345"
        val uri = URI(connectorString)
        Assert.assertTrue(ChainProvider.isSupport(uri))
        val chain = ChainProvider.createChain(uri)
        Assert.assertTrue(chain is EthereumChain)
    }
}