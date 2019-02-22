package org.starcoin.sirius.hub

import org.ethereum.util.blockchain.EtherUtil
import org.junit.AfterClass
import org.junit.BeforeClass
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.protocol.ethereum.loadEtherBaseKeyStoreFile
import org.starcoin.sirius.protocol.ethereum.scriptExec
import org.starcoin.sirius.util.WithLogging

class HubServerIntegrationEthereumChainTest :
    HubServerIntegrationTestBase<EthereumTransaction, EthereumAccount, EthereumChain>() {

    companion object : WithLogging() {
        @AfterClass
        @JvmStatic
        fun tearDown() {
            scriptExec("../sirius-chainconnector/scripts/docker.sh clean")
        }
    }

    override fun createChainAccount(amount: Long): EthereumAccount {
        val key = CryptoService.generateCryptoKey()
        val account = EthereumAccount(key)
        val etherbase = chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")
        val tx = chain.newTransaction(etherbase, account.address, EtherUtil.convert(amount, EtherUtil.Unit.ETHER))
        val hash = chain.submitTransaction(etherbase, tx)
        chain.waitTransactionProcessed(hash)
        return account
    }

    override fun createBlock() {
        chain.waitBlocks(1)
    }

    override fun createChain(configuration: Configuration): EthereumChain {
        scriptExec("../sirius-chainconnector/scripts/docker.sh run")
        Thread.sleep(4000)
        chain = EthereumChain()
        chain.waitBlocks(1)
        return chain
    }
}