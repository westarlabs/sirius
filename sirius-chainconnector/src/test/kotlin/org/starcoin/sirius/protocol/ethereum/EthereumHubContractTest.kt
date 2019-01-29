package org.starcoin.sirius.protocol.ethereum

import org.junit.Assert
import java.math.BigInteger

class EthereumHubContractTest : HubContractTestBase() {
    override val chain: EthereumBaseChain by lazy { EthereumChain() }

    override fun sendEther(to: EthereumAccount, value: BigInteger) {
        val from = EthereumServer.etherbaseAccount(chain as EthereumChain)
        val tx = chain.newTransaction(from, to.address, value)
        val hash = chain.submitTransaction(from, tx)
        chain.waitTransactionProcessed(hash)
        Assert.assertEquals(value, chain.getBalance(to.address))
    }
}