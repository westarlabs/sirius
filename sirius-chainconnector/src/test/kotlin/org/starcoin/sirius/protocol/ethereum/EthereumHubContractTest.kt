package org.starcoin.sirius.protocol.ethereum

import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class EthereumHubContractTest : HubContractTestBase() {
    companion object : WithLogging() {
        private val server = EthereumServer(false)

        @BeforeClass
        @JvmStatic
        fun setup() {
            server.ethStart()
            Thread.sleep(1000)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.ethStop()
        }
    }

    override val chain: EthereumBaseChain by lazy { EthereumChain() }


    init {
        while (true) {
            try {
                chain.waitBlocks(3)
            } catch (e: Exception) {
                HubContractTestBase.LOG.info("$e")
                continue
            }
            break
        }
    }

    override fun sendEther(to: EthereumAccount, value: BigInteger) {
        val from = EthereumServer.etherbaseAccount(chain as EthereumChain)
        val tx = chain.newTransaction(from, to.address, value)
        val hash = chain.submitTransaction(from, tx)
        chain.waitTransactionProcessed(hash)
        Assert.assertEquals(value, chain.getBalance(to.address))
    }
}