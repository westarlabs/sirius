package org.starcoin.sirius.protocol.ethereum

import org.junit.Before
import org.junit.Test
import org.junit.internal.Classes.getClass
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.util.FileUtil
import org.web3j.crypto.WalletFile
import org.web3j.utils.Numeric
import sun.misc.ClassLoaderUtil
import kotlin.properties.Delegates

class EthereumChainTest {
    private val rpcUrl = "http://127.0.0.1:8545"
    private var chain: EthereumChain by Delegates.notNull()
    private var Alice: CryptoKey by Delegates.notNull()

    @Before
    fun setUp() {
        /* Test Depend on the remove rpc server*/
        chain = EthereumChain(rpcUrl)
        val path = this.javaClass::class.java.getResource("/cryptoKey").file
        Alice = CryptoService.loadCryptoKey(FileUtil.readFile(path))
    }

    @Test
    fun testNewTransaction() {
        val Bob = CryptoService.generateCryptoKey()
        val tx: EthereumTransaction
        // chain.newTransaction(Alice,tx)
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