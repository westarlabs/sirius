package org.starcoin.sirius.protocol.ethereum

import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class EthereumHubContractTest : HubContractTestBase() {

    override val chain: EthereumBaseChain by lazy { EthereumChain() }

    private val etherbase: EthereumAccount by lazy{
        chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")}


    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun setup() {
            scriptExec("scripts/docker.sh run")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            scriptExec("scripts/docker.sh clean")
        }
    }

    init {
        while (true) {
            try {
                chain.waitBlocks(1)
            } catch (e: Exception) {
                EthereumChainTest.LOG.info("waiting block exception: $e")
                Thread.sleep(1000)
                continue
            }
            break
        }
    }

    override fun sendEther(to: EthereumAccount, value: BigInteger) {
        val tx = chain.newTransaction(etherbase, to.address, value)
        val hash = chain.submitTransaction(etherbase, tx)
        chain.waitTransactionProcessed(hash)
        Assert.assertEquals(value, chain.getBalance(to.address))
    }
}