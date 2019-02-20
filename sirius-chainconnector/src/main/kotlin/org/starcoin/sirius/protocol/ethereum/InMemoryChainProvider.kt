package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.chain.ChainProvider
import org.starcoin.sirius.protocol.Chain
import java.net.URI

class InMemoryChainProvider : ChainProvider {

    override fun isSupport(connectorURI: URI): Boolean {
        return connectorURI.scheme == scheme
    }

    override fun createChain(connectorURI: URI): Chain<*, *, *> {
        return InMemoryChain()
    }

    companion object {
        val scheme = "inmemory"
    }
}