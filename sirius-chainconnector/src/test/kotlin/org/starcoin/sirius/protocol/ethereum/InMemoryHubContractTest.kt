package org.starcoin.sirius.protocol.ethereum

import org.junit.Assert
import java.math.BigInteger

class InMemoryHubContractTest : HubContractTestBase() {
    override val chain: InMemoryChain = InMemoryChain(true)

    override fun sendEther(to: EthereumAccount, value: BigInteger) {
        chain.sb.sendEther(to.address.toBytes(), value)
        chain.waitBlocks()
        Assert.assertEquals(value, chain.getBalance(to.address))
    }
}