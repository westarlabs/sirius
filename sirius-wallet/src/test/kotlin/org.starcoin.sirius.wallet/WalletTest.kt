package org.starcoin.sirius.wallet

import io.grpc.inprocess.InProcessChannelBuilder
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.protocol.ethereum.contract.EthereumHubContract
import org.starcoin.sirius.wallet.core.ChannelManager
import org.starcoin.sirius.wallet.core.Wallet
import java.math.BigInteger
import java.util.logging.Logger
import kotlin.properties.Delegates

class WalletTest {

    private val logger = Logger.getLogger("test")

    private var chain: InMemoryChain by Delegates.notNull()
    private var contract: EthereumHubContract by Delegates.notNull()

    private var owner: EthereumAccount by Delegates.notNull()
    private var alice: EthereumAccount by Delegates.notNull()

    private var walletAlice : Wallet<EthereumTransaction,EthereumAccount> by Delegates.notNull()

    @Before
    @Throws(InterruptedException::class)
    fun before() {
        chain = InMemoryChain(true)

        owner = EthereumAccount(CryptoService.generateCryptoKey())
        alice = EthereumAccount(CryptoService.generateCryptoKey())

        val amount = EtherUtil.convert(100000, EtherUtil.Unit.ETHER)
        this.sendEther(owner.address, amount)
        this.sendEther(alice.address, amount)

        val args = ContractConstructArgs(HubRoot.EMPTY_TREE_HUBROOT)
        this.contract = chain.deployContract(owner,args)

        val hubChannel = InProcessChannelBuilder.forName("").build()

        val channelManager = ChannelManager(hubChannel)

        walletAlice= Wallet(this.contract.contractAddress,channelManager,chain,null,alice)
        commitHubRoot(0, BigInteger.ZERO)

    }

    fun sendEther(address: Address, amount: BigInteger) {
        chain.sb.sendEther(address.toBytes(), amount)
        chain.sb.createBlock()
        Assert.assertEquals(amount, chain.getBalance(address))
    }

    @Test
    fun testDeposit(){
        val amount=2000L
        walletAlice.deposit(amount)
        chain.sb.createBlock()

        Assert.assertEquals(amount, chain.getBalance(contract.contractAddress).toLong())
        Assert.assertEquals(walletAlice.balance().toLong(),amount)
    }

    @Test
    fun testWithdrawal() {
        testDeposit()

        val amount = 20L
        walletAlice.withdrawal(amount)

        chain.sb.createBlock()

        Assert.assertTrue(!walletAlice.hub.hubStatus.couldWithDrawal())
    }

    private fun commitHubRoot(eon: Int, amount: BigInteger): Hash {
        var height = chain.getBlockNumber().toLong()
        if (eon != 0) {
            if (height?.rem(8) != 0L) {
                var blockNumber = 8 - (height?.rem(8) ?: 0) - 1
                for (i in 0..blockNumber) {
                    chain.sb.createBlock()
                }
            }
        }
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0.toBigInteger(), amount)
        val root = HubRoot(node, eon)
        logger.info("current block height is :" + chain.getBlockNumber())
        logger.info(root.toJSON())
        owner.getAndIncNonce()
        val callResult = contract.commit(owner, root)
        return callResult
    }

    private fun waitToNextEon() {
        var height = chain.getBlockNumber().toLong()
        if (height?.rem(8) != 0L) {
            var blockNumber = 8 - (height?.rem(8) ?: 0) - 1
            for (i in 0..blockNumber) {
                chain.sb.createBlock()
            }
        }
    }
}
