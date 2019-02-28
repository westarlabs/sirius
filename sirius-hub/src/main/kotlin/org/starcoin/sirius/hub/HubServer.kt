package org.starcoin.sirius.hub

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import kotlin.properties.Delegates

class HubServer<A : ChainAccount>(
    val configuration: Configuration,
    val chain: Chain<out ChainTransaction, out Block<out ChainTransaction>, A>,
    val owner: A = configuration.ownerKeystore?.let { chain.createAccount(it, configuration.ownerKeystorePassword) }
        ?: chain.createAccount(configuration.ownerKey)
) {

    var grpcServer = GrpcServer(configuration)
    var contract: HubContract<A> by Delegates.notNull()
        private set

    var hubService: HubService by Delegates.notNull()

    fun start() {
        contract = configuration.contractAddress?.let { chain.loadContract(it) } ?: chain.deployContract(
            owner,
            ContractConstructArgs.DEFAULT_ARG
        )
        LOG.info("HubContract Address: ${contract.contractAddress}")
        hubService = HubServiceImpl(owner, chain, contract)
        val hubRpcService = HubRpcService(hubService)
        grpcServer.registerService(hubRpcService)
        hubService.start()
        grpcServer.start()
    }

    fun stop() {
        grpcServer.stop()
        hubService.stop()
    }

    fun awaitTermination() {
        grpcServer.awaitTermination()
    }

    companion object : WithLogging()
}
