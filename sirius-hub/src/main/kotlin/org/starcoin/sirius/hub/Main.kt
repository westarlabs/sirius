package org.starcoin.sirius.hub

import org.ethereum.util.blockchain.EtherUtil
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain


fun main(args: Array<String>) {
    val configuration = Configuration.loadConfiguration()
    val chain = InMemoryChain()
    val owner = EthereumAccount(configuration.ownerKey)
    chain.miningCoin(owner.address, EtherUtil.convert(Int.MAX_VALUE.toLong(), EtherUtil.Unit.ETHER))

    val hubServer = HubServer(configuration,chain,owner)
    hubServer.start()
    hubServer.awaitTermination()
}
