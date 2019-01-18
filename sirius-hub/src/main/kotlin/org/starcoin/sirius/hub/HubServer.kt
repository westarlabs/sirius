package org.starcoin.sirius.hub

import org.ethereum.util.blockchain.EtherUtil
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain

class HubServer(val configuration: Configuration) {

    var grpcServer = GrpcServer(configuration)


    fun start() {
        val owner = EthereumAccount(CryptoService.generateCryptoKey())
        val chain = InMemoryChain()
        chain.miningCoin(owner.address, EtherUtil.convert(1000000, EtherUtil.Unit.ETHER))
        val contract = chain.deployContract(owner, ContractConstructArgs.DEFAULT_ARG)
        val hubService = HubService(owner, chain, contract)
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
