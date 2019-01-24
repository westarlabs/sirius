package org.starcoin.sirius.hub

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.HubContract
import kotlin.properties.Delegates

class HubServer<T : ChainTransaction, A : ChainAccount>(val configuration: Configuration, val chain:Chain<T, out Block<T>, A>,val owner: A) {

    var grpcServer = GrpcServer(configuration)
    var contract : HubContract<A> by Delegates.notNull()
        private set

    fun start() {
        contract = chain.deployContract(owner, ContractConstructArgs.DEFAULT_ARG)
        val hubService = HubServiceImpl(owner, chain, contract)
        val hubRpcService = HubRpcService(hubService)
        grpcServer.registerService(hubRpcService)
        hubService.start()
        grpcServer.start()
    }

    fun stop() {
        grpcServer.stop()
    }

    fun awaitTermination() {
        grpcServer.awaitTermination()
    }
}
