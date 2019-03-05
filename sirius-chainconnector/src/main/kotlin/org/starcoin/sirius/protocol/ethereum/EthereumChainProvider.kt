package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.chain.ChainProvider
import java.net.URI

class EthereumChainProvider : ChainProvider {
    override fun isSupport(connectorURI: URI): Boolean {
        return connectorURI.scheme == scheme
    }

    override fun createChain(connectorURI: URI): EthereumChain {
        val url = connectorURI.schemeSpecificPart
        return EthereumChain(webSocket = url)
    }

    companion object {
        val scheme = "ethereum"
    }
}