package org.starcoin.sirius.hub

import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.eth.core.ether
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.protocol.ethereum.loadEtherBaseKeyStoreFile
import org.starcoin.sirius.protocol.ethereum.scriptExec
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import kotlin.properties.Delegates

class HubServerIntegrationEthereumChainTest :
    HubServerIntegrationTestBase<EthereumTransaction, EthereumAccount, EthereumChain>() {

    companion object : WithLogging() {
        @AfterClass
        @JvmStatic
        fun tearDown() {
            scriptExec("../sirius-chainconnector/scripts/docker.sh clean")
        }

        @BeforeClass
        @JvmStatic
        fun setUp() {
            scriptExec("../sirius-chainconnector/scripts/docker.sh run")
            Thread.sleep(5000)
        }
    }

    var systemAccount: EthereumAccount by Delegates.notNull()

    override val waitTimeOutMillis: Long = 16000

    override fun doBefore() {
        systemAccount = this.createChainAccount(10000)
    }

    override fun createChainAccount(amount: Long): EthereumAccount = runBlocking {
        val key = CryptoService.generateCryptoKey()
        val account = EthereumAccount(key)
        val etherbase = chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")
        val tx = chain.newTransaction(etherbase, account.address, amount.ether.inWei.value)
        val txDeferred = chain.submitTransaction(etherbase, tx)
        txDeferred.await()
        account
    }

    override fun createBlock() = runBlocking {
        //submit tx to trigger new block.
        val txDeferred = chain.submitTransaction(
            systemAccount,
            chain.newTransaction(systemAccount, Address.DUMMY_ADDRESS, BigInteger.ONE)
        )
        txDeferred.awaitTimout()
        return@runBlocking
        //chain.waitBlocks(1)
    }

    override fun createChain(configuration: Config): EthereumChain {
        //scriptExec("../sirius-chainconnector/scripts/docker.sh run")
        //Thread.sleep(6000)
        chain = EthereumChain()
        return chain
    }
}