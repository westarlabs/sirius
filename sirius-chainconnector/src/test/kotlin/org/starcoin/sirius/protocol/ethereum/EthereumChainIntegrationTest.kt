package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.util.WithLogging


class EthereumChainIntegrationTest {

    private val chain: EthereumChain by lazy { EthereumChain() }
    private val alice = EthereumAccount(CryptoService.generateCryptoKey())
    private val etherbase: EthereumAccount =
        chain.createAccount(loadEtherBaseKeyStoreFile("/tmp/geth_data/keystore"), "")

    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun setup() {
            scriptExec("scripts/docker.sh run --dev.period 1")
            Thread.sleep(4000)
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
    fun testSubmitTransaction() = runBlocking {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)

        val deferred = chain.submitTransaction(etherbase, tx)
        Assert.assertEquals(deferred.txHash, tx.txHash())
        Assert.assertEquals(deferred.txHash, tx.hash())
        deferred.await()
        Assert.assertEquals(transAmount, chain.getBalance(alice.address))
    }

    @Test
    fun testFindTransaction() = runBlocking {
        val transAmount = 100.toBigInteger()
        val tx = chain.newTransaction(etherbase, alice.address, transAmount)
        tx.sign(etherbase.key as EthCryptoKey)
        tx.verify()
        LOG.info("tx from:${tx.from}")
        Assert.assertNotNull(tx.from)
        val tx1 = chain.newTransaction(etherbase, alice.address, transAmount)
        tx1.verify()
        val deferred = chain.submitTransaction(etherbase, tx1)
        val receipt = deferred.await()
        val txFind = chain.findTransaction(deferred.txHash)
        receipt.logs?.forEach {
            LOG.info(it.toString())
        }
        Assert.assertNotNull(txFind?.from)
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        Assert.assertNotNull(block)
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

    @Test
    fun testWatchBlock() {
        var startNum = 5.toBigInteger()
        chain.waitBlocks(10)
        val blockch = chain.watchBlock(startNum)
        repeat(10) {
            runBlocking {
                val block = blockch.receive()
                LOG.info("block height:${block.height.toBigInteger()}")
                Assert.assertEquals(startNum, block.height.toBigInteger())
                startNum = startNum.inc()
            }
        }
    }

    @Test
    fun testWatchBlock1() {
        var startNum = 11.toBigInteger()
        chain.waitBlocks(10)
        val blockch = chain.watchBlock(startNum)
        repeat(10) {
            runBlocking {
                val block = blockch.receive()
                LOG.info("block height:${block.height.toBigInteger()}")
                Assert.assertEquals(startNum, block.height.toBigInteger())
                startNum = startNum.inc()
            }
        }
    }

    @Test
    fun testwatchBlock2(){
        var startNum = 11.toBigInteger()
        chain.waitBlocks(startNum.toInt())
        val blockch = chain.watchBlock()
        repeat(10) {
            runBlocking {
                val block = blockch.receive()
                LOG.info("block height:${block.height.toBigInteger()}")
                Assert.assertTrue(block.height.toBigInteger()>=startNum)
                startNum = startNum.inc()
            }
        }
    }
}