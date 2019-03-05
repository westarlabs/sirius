package org.starcoin.sirius.hub

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import kotlin.properties.Delegates

class HubServer<A : ChainAccount>(
    val config: Config,
    val chain: Chain<out ChainTransaction, out Block<out ChainTransaction>, A>,
    val owner: A = config.ownerKeystore?.let {
        chain.createAccount(
            it,
            config.accountIDOrAddress,
            config.ownerKeystorePassword
        ).apply { LOG.info("Create owner account ${this.address} by $it, ${config.accountIDOrAddress}") }
    }
        ?: chain.createAccount(config.ownerKey),
    val dataStoreFactory: DataStoreFactory = MapDataStoreFactory()
) {

    var grpcServer = GrpcServer(config)
    var contract: HubContract<A> by Delegates.notNull()
        private set

    var hubService: HubService by Delegates.notNull()

    fun start() {
        contract = config.contractAddress?.let { chain.loadContract(it) } ?: chain.deployContract(
            owner,
            ContractConstructArgs(config.blocksPerEon, HubRoot.DUMMY_HUB_ROOT)
        ).apply {
            config.contractAddress = this.contractAddress
            config.store()
        }
        LOG.info("HubContract Address: ${contract.contractAddress}")
        hubService = HubServiceImpl(owner, chain, contract, dataStoreFactory)
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
