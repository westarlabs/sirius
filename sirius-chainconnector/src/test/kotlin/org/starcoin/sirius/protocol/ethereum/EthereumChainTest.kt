package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.utils.Numeric
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates


/* XXX: Move those properties to test configure*/
const val ALICE_KEY = "2b9221b2df843566d8292f7c3c2157144d4ca4e9c87c4c5dc4b055c1129dec3e"
const val BOB_KEY = "eccb6cf0a844702f35f212ac899c75ed22e3674c2938691dd7d528cbf6db4510"
const val RPC_URL = "http://39.96.89.55:8545"

class EthereumChainTest {
    private var chain: Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount> by Delegates.notNull()
    private var alice: EthereumAccount by Delegates.notNull()
    private var bob: EthereumAccount by Delegates.notNull()


    @Before
    fun setUp() {
        /* Test Depend on the remove rpc server*/
        val ethchain = EthereumChain(RPC_URL)
        chain = ethchain as Chain<ChainTransaction, Block<ChainTransaction>, ChainAccount>
        val genAcount = { prvKey: String ->
            CryptoService.loadCryptoKey(Numeric.hexStringToByteArray(prvKey)).let {
                EthereumAccount(
                    it,
                    AtomicLong(ethchain.getNonce(it.address).longValueExact())
                )
            }
        }
        alice = genAcount(ALICE_KEY)
        bob = genAcount(BOB_KEY)
    }

    @Test
    fun testSubmitTransaction() {
        val tx = chain.newTransaction(alice, bob.address, 1.toBigInteger())
        val hash = chain.submitTransaction(alice, tx)
        val expectTx = chain.findTransaction(hash)
        Assert.assertEquals(tx, expectTx)
    }

    @Test
    fun testWatchTransactions() {

    }

    @Test
    fun testGetBlock() {
        val block = chain.getBlock()
        println("the block height current is ${block!!.height}")
    }

}
