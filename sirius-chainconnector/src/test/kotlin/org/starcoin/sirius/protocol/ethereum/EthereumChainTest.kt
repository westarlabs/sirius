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
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.crypto.WalletUtils
import java.io.File
import kotlin.math.exp
import kotlin.properties.Delegates


class EthereumChainTest : EtherumContainer() {
    private var chain: Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount> by Delegates.notNull()
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val bob = EthereumAccount(CryptoService.generateCryptoKey())
    private var etherbase: EthereumAccount by Delegates.notNull()

    @Before
    fun setUp() {
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
        var receipt: Receipt?
        for (i in 1..4) {
            Thread.sleep(1000)
            receipt = chain.getTransactionReceipts(ArrayList<Hash>(1).apply { this.add(hash) })[0]
            if (receipt != null) break
        }
        Assert.assertEquals(1, receipt!!.status)
        Assert.assertEquals(1.toBigInteger(), chain.getBalance(alice.address))
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

    fun ethStart() {
        scriptExec("clean")
        scriptExec("run")
    }

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
