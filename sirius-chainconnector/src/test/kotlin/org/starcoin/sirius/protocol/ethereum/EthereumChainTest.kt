package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.ChainEvent
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.util.WithLogging
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates


class EthereumChainTest : EthereumServer(true) {
    companion object : WithLogging()

    init {
        this.ethStart()
    }

    private val chain: EthereumChain by lazy { EthereumChain() }
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val etherbase: EthereumAccount by lazy { etherbaseAccount(chain) }

    @After
    fun tearDown() {
        this.ethStop()
    }

    @Test
    fun testSubmitTransaction() {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)

        val hash = chain.submitTransaction(etherbase, tx)
        Assert.assertEquals(hash, tx.txHash())
        chain.waitTransactionProcessed(hash)
        Assert.assertEquals(transAmount, chain.getBalance(alice.address))
    }

    @Test
    fun testFindTransaction() {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount) as EthereumTransaction
        tx.sign(etherbase.key as EthCryptoKey)
        tx.verify()
        LOG.info("tx from:${tx.from}")
        Assert.assertNotNull(tx.from)
        val tx1 = chain.newTransaction(etherbase, alice.address, transAmount) as EthereumTransaction
        tx1.verify()
        val hash = chain.submitTransaction(etherbase, tx1)
        val txFind = chain.findTransaction(hash)
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
                LOG.info("tx recived ${txResult.tx.toString()}")
                Assert.assertEquals(transAmount, txResult.tx.amount)
                Assert.assertEquals(etherbase.address, txResult.tx.from)
                Assert.assertEquals(alice.address, txResult.tx.to)
            }
        }

    }

    fun testWatchEvents() {
        val contracAddress: Address = Address.DUMMY_ADDRESS
        val events = listOf(ChainEvent.MockTopic)
        chain.watchEvents(contracAddress, events)
    }
}