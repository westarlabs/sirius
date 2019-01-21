package org.starcoin.sirius.protocol.ethereum

import org.junit.*
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.web3j.crypto.WalletUtils
import java.io.File
import kotlin.properties.Delegates


class EthereumChainTest : EtherumContainer() {
    private var chain: Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount> by Delegates.notNull()
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val bob = EthereumAccount(CryptoService.generateCryptoKey())
    private var etherbase: EthereumAccount by Delegates.notNull()

    @Before
    fun setUp() {
        /*
         XXX: Rquired to build image outside by
         running `scrips/docker.sh build`.
        */
        this.ethStart()
        etherbase = EthereumAccount(etherbase())
        val ethchain = EthereumChain()
        chain = ethchain as Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount>
    }

    @After
    fun tearDown() {
        this.ethStop()
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

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        Assert.assertNotNull(block)
    }
}

open class EtherumContainer {
    private val keystore = "/tmp/geth_data/keystore"
    private val script = "scripts/docker.sh"
    private val etherbasePasswd = "starcoinmakeworldbetter"

    fun ethStart() = scriptExec("run")

    fun ethStop() = scriptExec("clean")

    fun etherbase(): CryptoKey {
        val credentials = WalletUtils.loadCredentials(
            etherbasePasswd,
            File(keystore).let {
                while (!it.exists() || it.name.contentEquals("tmp")) {
                    Thread.sleep(1000)
                }; it
            }.listFiles().first()
        )
        return CryptoService.loadCryptoKey(credentials.ecKeyPair.privateKey.toByteArray())
    }

    private fun scriptExec(cmd: String) {
        val process = Runtime.getRuntime().exec("$script $cmd")
        val exit = process.waitFor()
        if (exit != 0) Assert.fail(process.errorStream.bufferedReader().use { it.readText() })
    }
}
