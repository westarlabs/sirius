package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.utils.Numeric
import java.lang.Thread.sleep
import kotlin.properties.Delegates


/* XXX: Move those properties to test configure*/
const val AlicePrvKey = "2b9221b2df843566d8292f7c3c2157144d4ca4e9c87c4c5dc4b055c1129dec3e"
const val BobPrvKey = "eccb6cf0a844702f35f212ac899c75ed22e3674c2938691dd7d528cbf6db4510"
const val rpcUrl = "http://39.96.89.55:8545"

class EthereumChainTest {
    private var chain: EthereumChain by Delegates.notNull()
    private val alice = EthereumAccount(CryptoService.loadCryptoKey(Numeric.hexStringToByteArray(AlicePrvKey)))
    private val bob = EthereumAccount(CryptoService.loadCryptoKey(Numeric.hexStringToByteArray(BobPrvKey)))

    @Before
    fun setUp() {
        /* Test Depend on the remove rpc server*/
        chain = EthereumChain(rpcUrl)
    }

    @Test
    fun testNewTransaction() {
        val tx = EthereumTransaction(
            bob.address, alice.getNonce(), 1000.toBigInteger(),
            100000.toBigInteger(), 1.toBigInteger()
        )
        chain.submitTransaction(alice, tx)
    }

    @Test
    fun testWatchTransactions() {
        GlobalScope.launch {
            chain.submitTransaction(
                alice, EthereumTransaction(
                    bob.address, alice.getNonce(), 1000.toBigInteger(),
                    100000.toBigInteger(), 1.toBigInteger()
                )
            )
            delay(1000)
        }
    }

    @Test
    fun testfindTransaction() {
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        println("the block height current is ${block!!.height}")
    }

    @Test
    fun testGetBalance() {
        println("balance of bob is ${chain.getBalance(bob.address)}")
    }

    @Test
    fun testContract() {
        createTestContract()
    }

    private fun createTestContract() {
        val data =
            "6080604052348015600f57600080fd5b5060be8061001e6000396000f3fe6080604052600436106038577c01000000000000000000000000000000000000000000000000000000006000350463b214faa58114603d575b600080fd5b605760048036036020811015605157600080fd5b50356059565b005b60408051348152905133917fe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c919081900360200190a25056fea165627a7a72305820c67e9008687d656c665ae79c4fbe2d8ec17dd117a845c566753b9ecd8095d8870029".toByteArray()
        val tx = EthereumTransaction(
            alice.getNonce(), 1000.toBigInteger(), 100000.toBigInteger(), data
        )
        chain.submitTransaction(alice, tx)
        println(Numeric.toHexString(tx.tx.contractAddress))
    }

    @Test
    fun testChannel() {
        suspend fun newJob(): Channel<Int> {
            val ch = Channel<Int>(10)
            GlobalScope.launch {
                repeat(100) {
                    ch.send(it)
                }
            }

            return ch
        }
        
        runBlocking {
            val ch = newJob()
            sleep(2000)
            repeat(100) {
                println(ch.receive())                        
            }
        }
    }
}
