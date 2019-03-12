package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class EthereumHubContractIntegrationTest : HubContractTestBase() {

    override val chain: EthereumBaseChain by lazy { EthereumChain() }

    private val etherbase: EthereumAccount by lazy{
        chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")}


    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun setup() {
            scriptExec("../scripts/docker.sh run --dev.period 1")
            Thread.sleep(4000)
        }


        @AfterClass
        @JvmStatic
        fun tearDown() {
            scriptExec("../scripts/docker.sh clean")
        }
    }

    init {
        while (true) {
            try {
                chain.waitBlocks(1)
            } catch (e: Exception) {
                EthereumChainIntegrationTest.LOG.info("waiting block exception: $e")
                Thread.sleep(1000)
                continue
            }
            break
        }
    }

    override fun sendEther(to: EthereumAccount, value: BigInteger) = runBlocking {
        val tx = chain.newTransaction(etherbase, to.address, value)
        val txDeferred = chain.submitTransaction(etherbase, tx)
        txDeferred.awaitTimoutOrNull()
        Assert.assertEquals(value, chain.getBalance(to.address))
    }
}