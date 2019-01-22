package org.starcoin.sirius.protocol.ethereum

import org.junit.*
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.web3j.crypto.WalletUtils
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates


class EthereumChainTest : EtherumServer(true) {
    private var chain: Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount> by Delegates.notNull()
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val bob = EthereumAccount(CryptoService.generateCryptoKey())
    private var etherbase: EthereumAccount by Delegates.notNull()
    private var ethchain: EthereumChain by Delegates.notNull()

    @Before
    fun setUp() {
        this.ethStart()
        ethchain = EthereumChain()
        val etherbaseKey = etherbaseKey()
        etherbase = EthereumAccount(etherbaseKey, AtomicLong(ethchain.getNonce(etherbaseKey.address).longValueExact()))
        chain = ethchain as Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount>
    }

    @After
    fun tearDown() {
        this.ethStop()
    }

    @Test
    fun testSubmitTransaction() {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)
        val hash = chain.submitTransaction(etherbase, tx)
        var receipt: Receipt? = null
        while (true) { // Wait transaction being processed
            Thread.sleep(1000)
            receipt = chain.getTransactionReceipts(ArrayList<Hash>(1).apply { this.add(hash) })[0]
            if (receipt != null) break
        }
        Assert.assertEquals(true, receipt!!.status)
        Assert.assertEquals(transAmount, chain.getBalance(alice.address))
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        Assert.assertNotNull(block)
    }
}

open class EtherumServer(var started: Boolean) {
    private val keystore = "/tmp/geth_data/keystore"
    private val script = "scrpts/docker.sh"
    private val etherbasePasswd = "starcoinmakeworldbetter"

    fun ethStart() {
        if (this.started) return
        scriptExec("clean")
        scriptExec("run")
    }

    fun ethStop() {
        if (this.started) return
        scriptExec("clean")
    }

    fun etherbaseKey(): CryptoKey {
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