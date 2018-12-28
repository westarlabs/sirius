package org.starcoin.sirius.protocol.ethereum

import org.ethereum.crypto.ECKey
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.util.KeyPairUtil
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric
import java.security.KeyPair
import kotlin.properties.Delegates

class EthereumChainTest {
    private val rpcUrl = "http://127.0.0.1:8545"
    private var chain: EthereumChain by Delegates.notNull()

    @Before
    fun setUp() {
        /* Test Depend on the remove rpc server*/
        chain = EthereumChain(rpcUrl)
    }

    @Test
    fun testNewTransaction() {
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
}