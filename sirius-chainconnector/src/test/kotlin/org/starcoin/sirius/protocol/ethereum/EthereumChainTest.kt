package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.ChainEvent
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.WithLogging
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Event
import org.web3j.crypto.Hash
import java.util.*


class EthereumChainTest {

    private val chain: EthereumChain by lazy { EthereumChain() }
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val etherbase: EthereumAccount =
        chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")

    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun setup() {
            scriptExec("scripts/docker.sh run")
            Thread.sleep(2000)
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
                LOG.info("waiting block exception: $e")
                Thread.sleep(1000)
                continue
            }
            break
        }
    }

    @Test
    fun testSubmitTransaction() {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)

        val hash = chain.submitTransaction(etherbase, tx)
        Assert.assertEquals(hash, tx.txHash())
        Assert.assertEquals(hash, tx.hash())
        chain.waitTransactionProcessed(hash)
        Assert.assertEquals(transAmount, chain.getBalance(alice.address))
    }

    @Test
    fun testFindTransaction() {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)
        tx.sign(etherbase.key as EthCryptoKey)
        tx.verify()
        LOG.info("tx from:${tx.from}")
        Assert.assertNotNull(tx.from)
        val tx1 = chain.newTransaction(etherbase, alice.address, transAmount)
        tx1.verify()
        val hash = chain.submitTransaction(etherbase, tx1)
        val txFind = chain.findTransaction(hash)
        val receipt = chain.waitTransactionProcessed(hash)
        receipt!!.logs?.forEach {
            LOG.info(it)
        }
        Assert.assertNotNull(txFind?.from)
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        Assert.assertNotNull(block)
    }

    @Test
    fun testWatchBlock() {
        val ch = chain.watchBlock()
        runBlocking {
            for (c in 5 downTo 0) {
                val block = ch.receive()
                LOG.info("block info: height:${block.height},hash: ${block.blockHash()}")
                Assert.assertNotNull(block.height)
                Assert.assertNotNull(block.blockHash())
            }
        }
    }

    @Test
    fun testWatchTransactions() {
        val ch = chain.watchTransactions {
            it.tx.to == alice.address && it.tx.from == etherbase.address
        }
        Thread.sleep(2000)
        val transAmount = 100.toBigInteger()
        GlobalScope.launch {
            for (i in 5 downTo 0) {
                val hash = chain.submitTransaction(
                    etherbase,
                    chain.newTransaction(etherbase, alice.address, transAmount)
                )
                LOG.info("tx hash is $hash")
            }
        }
        runBlocking {
            for (i in 5 downTo 0) {
                val txResult = ch.receive()
                LOG.info("tx recived ${txResult.tx}")
                Assert.assertEquals(transAmount, txResult.tx.amount)
                Assert.assertEquals(etherbase.address, txResult.tx.from)
                Assert.assertEquals(alice.address, txResult.tx.to)
            }
        }
        ch.cancel()

    }
}