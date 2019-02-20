package org.starcoin.sirius.chain

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.util.WithLogging
import java.net.URI
import java.util.*

interface ChainProvider{

    fun isSupport(connectorURI: URI): Boolean

    fun createChain(connectorURI: URI): Chain<out ChainTransaction, out Block<out ChainTransaction>, out ChainAccount>

    companion object : ChainProvider, WithLogging(){
        const val scheme = "chain"
        override fun isSupport(connectorURI: URI): Boolean {
            if (connectorURI.scheme != scheme) {
                return false
            }
            val chainConnectorURI = URI(connectorURI.schemeSpecificPart)
            val loaders = loadProvider()
            while (loaders.hasNext()) {
                val provider = loaders.next()
                if (provider.isSupport(chainConnectorURI)) {
                    return true
                }
            }
            return false
        }

        override fun createChain(connectorURI: URI): Chain<out ChainTransaction, out Block<out ChainTransaction>, out ChainAccount> {
            if (connectorURI.scheme != scheme) {
                throw UnsupportedChainURIException(connectorURI)
            }
            LOG.info("Create chain connector by URI:$connectorURI")
            val loaders = loadProvider()
            val chainConnectorURI = URI(connectorURI.schemeSpecificPart)
            while (loaders.hasNext()) {
                val provider = loaders.next()
                if (provider.isSupport(chainConnectorURI)) {
                    return provider.createChain(chainConnectorURI)
                }
            }
            throw UnsupportedChainURIException(connectorURI)
        }

        private fun loadProvider():Iterator<ChainProvider>{
            return ServiceLoader
                .load(ChainProvider::class.java).iterator()
        }
    }
}

class UnsupportedChainURIException(connectorURI: URI) : RuntimeException("Unsupported Chain URI:$connectorURI")