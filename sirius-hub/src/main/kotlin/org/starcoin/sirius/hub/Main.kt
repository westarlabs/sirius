package org.starcoin.sirius.hub

import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.chain.ChainProvider


fun main(args: Array<String>) = runBlocking {
    val configuration = Configuration.loadConfiguration()
    val chain = ChainProvider.createChain(configuration.connector)

    val hubServer = HubServer(configuration, chain)
    hubServer.start()
    hubServer.awaitTermination()
}
