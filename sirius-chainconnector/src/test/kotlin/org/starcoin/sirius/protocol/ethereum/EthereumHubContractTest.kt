package org.starcoin.sirius.protocol.ethereum

import kotlinx.serialization.ImplicitReflectionSerializer
import org.junit.Assert
import org.junit.Test
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

    @Test
    @ImplicitReflectionSerializer
    override fun testHubInfo() {
        super.testHubInfo()
    }

    @Test
    @ImplicitReflectionSerializer
    override fun testCurrentEon() {
        super.testCurrentEon()
    }

    @Test
    @ImplicitReflectionSerializer
    override fun testDeposit() {
        super.testDeposit()
    }

    @Test
    @ImplicitReflectionSerializer
    override fun testWithDrawal() {
        super.testWithDrawal()
    }

    @Test
    @ImplicitReflectionSerializer
    override fun testCommit() {
        super.testCommit()
    }

    @Test
    @ImplicitReflectionSerializer
    override fun testBalanceUpdateChallenge() {
        super.testBalanceUpdateChallenge()
    }


    @Test
    @ImplicitReflectionSerializer
    override fun testTransferChallenge() {
        super.testTransferChallenge()
    }
}