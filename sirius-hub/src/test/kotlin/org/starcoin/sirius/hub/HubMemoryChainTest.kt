package org.starcoin.sirius.hub

import org.ethereum.util.blockchain.EtherUtil
import org.junit.Ignore
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain

@Ignore
class HubMemoryChainTest :
    HubTestBase<EthereumTransaction, EthereumAccount, InMemoryChain>() {

    override fun createChainAccount(amount: Long): EthereumAccount {
        val key = CryptoService.generateCryptoKey()
        val account = EthereumAccount(key)
        chain.tryMiningCoin(account, EtherUtil.convert(amount, EtherUtil.Unit.ETHER))
        return account
    }

    override fun createBlock() {
        chain.createBlock()
    }

    override fun createChain(configuration: Config): InMemoryChain {
        return InMemoryChain()
    }
}
