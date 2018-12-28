package org.starcoin.sirius.protocol.ethereum

import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.utils.Numeric
import kotlin.properties.Delegates

const val AlicePrvKey = "2b9221b2df843566d8292f7c3c2157144d4ca4e9c87c4c5dc4b055c1129dec3e"
const val BobPrvKey = "eccb6cf0a844702f35f212ac899c75ed22e3674c2938691dd7d528cbf6db4510"
const val rpcUrl = "http://39.96.89.55:8545"

class EthereumChainTest {
    private var chain: EthereumChain by Delegates.notNull()
    private val Alice = CryptoService.loadCryptoKey(Numeric.hexStringToByteArray(AlicePrvKey))
    private val Bob = CryptoService.loadCryptoKey(Numeric.hexStringToByteArray(BobPrvKey))


    @Before
    fun setUp() {
        /* Test Depend on the remove rpc server*/
        chain = EthereumChain(rpcUrl)
    }

    @Test
    fun testNewTransaction() {
        val Bob = CryptoService.generateCryptoKey()
        val tx = EthereumTransaction(
            Bob.getAddress(), chain.getNonce(Alice.getAddress()), 1000,
            100000, 1, null
        )
        chain.newTransaction(Alice, tx)
    }

    @Test
    fun testfindTransaction() {
        TODO()
    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        println("the block height current is ${block!!.height}")
    }

    @Test
    fun testGetBalance() {
        println("balance of bob is ${chain.getBalance(Bob.getAddress())}")
    }
}