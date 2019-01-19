package org.starcoin.sirius.wallet

import com.google.protobuf.Empty
import io.grpc.Channel
import io.grpc.inprocess.InProcessChannelBuilder
import org.ethereum.util.blockchain.EtherUtil
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.hub.Configuration
import org.starcoin.sirius.hub.HubServer
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
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
    private var contract: HubContract<EthereumAccount> by Delegates.notNull()

    private var owner: EthereumAccount by Delegates.notNull()
    private var alice: EthereumAccount by Delegates.notNull()

    private var walletAlice : Wallet<EthereumTransaction,EthereumAccount> by Delegates.notNull()

    private var hubServer: HubServer<EthereumTransaction,EthereumAccount> by Delegates.notNull()

    private val configuration = Configuration.configurationForUNIT()

    private var hubChannel : Channel by Delegates.notNull()

    private var stub : HubServiceGrpc.HubServiceBlockingStub by Delegates.notNull()


    @Before
    @Throws(InterruptedException::class)
    fun before() {
        chain = InMemoryChain(true)

        val owner = EthereumAccount(configuration.ownerKey)
        chain.miningCoin(owner.address, EtherUtil.convert(Int.MAX_VALUE.toLong(), EtherUtil.Unit.ETHER))
        alice = EthereumAccount(CryptoService.generateCryptoKey())

        val amount = EtherUtil.convert(100000, EtherUtil.Unit.ETHER)
        this.sendEther(alice.address, amount)

        //this.contract = chain.deployContract(owner, ContractConstructArgs.DEFAULT_ARG)

        hubChannel = InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build()
        stub = HubServiceGrpc.newBlockingStub(hubChannel)

        val channelManager = ChannelManager(hubChannel)


        hubServer = HubServer(configuration,chain,owner)
        hubServer.start()
        contract = hubServer.contract

        walletAlice= Wallet(this.contract.contractAddress,channelManager,chain,null,alice)

    }

    fun sendEther(address: Address, amount: BigInteger) {
        chain.sb.sendEther(address.toBytes(), amount)
        chain.sb.createBlock()
        Assert.assertEquals(amount, chain.getBalance(address))
    }

    fun waitHubReady(stub: HubServiceGrpc.HubServiceBlockingStub) {
        var hubInfo = stub.getHubInfo(Empty.newBuilder().build())
        while (!hubInfo.ready) {
            logger.info("waiting hub ready:")
            Thread.sleep(100)
            hubInfo = stub.getHubInfo(Empty.newBuilder().build())
        }
    }

    @After
    fun after() {
        hubServer.stop()
    }

    @Test
    fun testDeposit(){
        testReg()

        waitToNextEon()

        val amount=2000L
        walletAlice.deposit(amount)
        chain.sb.createBlock()

        Assert.assertEquals(amount, chain.getBalance(contract.contractAddress).toLong())
        Assert.assertEquals(walletAlice.balance().toLong(),amount)

        val account=stub.getHubAccount(alice.address.toProto())

        println(account)
        Assert.assertEquals(HubAccount.parseFromProtoMessage(account).deposit.toLong(),amount)

    }


    @Test
    fun testReg(){
        waitHubReady(stub)

        var update=walletAlice.register()

        Assert.assertNotNull(update)
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
