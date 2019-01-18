package org.starcoin.sirius.protocol.ethereum

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.web3j.crypto.WalletUtils
import java.io.File
import kotlin.properties.Delegates


/* XXX: Move those properties to test configure*/
const val RPC_URL = "http://127.0.0.1:8545"
const val ETHERBASE_PASSWD = "starcoinmakeworldbetter"

class EthereumChainTest {
    private var chain: Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount> by Delegates.notNull()
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val bob = EthereumAccount(CryptoService.generateCryptoKey())
    private val etherbase = EthereumAccount(etherbase())

    @Before
    fun setUp() {
        /*
         XXX: Setup go-etheruem container here. For now, required started outside by
         running `scrips/docker.sh build && script/docker.sh run`.
        */
        val ethchain = EthereumChain(RPC_URL)
        chain = ethchain as Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount>

    }

    @Test
    fun testSubmitTransaction() {
        val balance = chain.getBalance(etherbase.address)
        Assert.assertNotSame(0.toBigInteger(), balance)
        val tx = chain.newTransaction(etherbase, alice.address, 1.toBigInteger())
        val hash = chain.submitTransaction(etherbase, tx)
        val expectTx = chain.findTransaction(hash)
        Assert.assertNotNull(expectTx)
    }


    private fun etherbase(): CryptoKey {
        val credentials = WalletUtils.loadCredentials(
            ETHERBASE_PASSWD,
            File(this.javaClass.getResource("/keystore").toURI()).listFiles().first()
        )
        return CryptoService.loadCryptoKey(credentials.ecKeyPair.privateKey.toByteArray())
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        Assert.assertNotNull(block)
    }
}
