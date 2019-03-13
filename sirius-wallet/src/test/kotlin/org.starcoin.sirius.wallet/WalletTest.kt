package org.starcoin.sirius.wallet

import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.ethereum.util.blockchain.EtherUtil
import org.junit.*
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.ContractHubInfo
import org.starcoin.sirius.core.Eon
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.hub.Config
import org.starcoin.sirius.hub.HubServer
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.wallet.core.ClientAccount
import org.starcoin.sirius.wallet.core.ClientEventType
import org.starcoin.sirius.wallet.core.ResourceManager
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
    private var bob: EthereumAccount by Delegates.notNull()

    private var walletAlice : Wallet<EthereumTransaction,EthereumAccount> by Delegates.notNull()
    private var walletBob : Wallet<EthereumTransaction,EthereumAccount> by Delegates.notNull()

    private var hubServer: HubServer<EthereumAccount> by Delegates.notNull()

    private val configuration = Config.configurationForUNIT()

    private var hubChannel : ManagedChannel by Delegates.notNull()

    private var stub : HubServiceGrpc.HubServiceBlockingStub by Delegates.notNull()

    private var hubInfo:ContractHubInfo by Delegates.notNull()

    @Before
    @Throws(InterruptedException::class)
    fun before() {
        chain = InMemoryChain(true)

        owner = EthereumAccount(CryptoService.generateCryptoKey())
        alice = EthereumAccount(CryptoService.generateCryptoKey())
        bob = EthereumAccount(CryptoService.generateCryptoKey())

        val amount = EtherUtil.convert(10000000, EtherUtil.Unit.ETHER)

        this.sendEther(alice, amount)
        this.sendEther(bob, amount)
        chain.tryMiningCoin(owner, EtherUtil.convert(Int.MAX_VALUE.toLong(), EtherUtil.Unit.ETHER))

        hubChannel = InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build()
        stub = HubServiceGrpc.newBlockingStub(hubChannel)
        ResourceManager.hubChannel=hubChannel

        hubServer = HubServer(configuration, chain,owner)
        hubServer.start()
        contract = hubServer.contract

        walletAlice= Wallet(chain, ClientAccount(alice,"alice"),null)
        walletAlice.initMessageChannel()

        walletBob= Wallet(chain,ClientAccount(bob,"bob"),null)
        walletBob.initMessageChannel()

        hubInfo= contract.queryHubInfo(alice)

        logger.info("block height before test is ${chain.getBlockNumber()}")
    }

    fun sendEther(address: EthereumAccount, amount: BigInteger) {
        //chain.sb.sendEther(address.toBytes(), amount)
        //chain.newTransaction(owner,address,amount)
        chain.tryMiningCoin(address,amount)
        createBlocks(1)

        Assert.assertEquals(amount, chain.getBalance(address.address))
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
        hubChannel.shutdownNow()
        runBlocking {
            walletAlice.close()
            walletBob.close()
        }
        ResourceManager.instance("alice").cleanData()
        ResourceManager.instance("bob").cleanData()
    }

    @Test
    fun testDeposit(){
        testReg()

        val amount = EtherUtil.convert(2000, EtherUtil.Unit.ETHER)
        deposit(amount)

        var account=stub.getHubAccount(alice.address.toProto())

        Assert.assertEquals(HubAccount.parseFromProtoMessage(account).deposit,amount)

        account=stub.getHubAccount(bob.address.toProto())
        Assert.assertEquals(HubAccount.parseFromProtoMessage(account).deposit,amount)

    }

    fun deposit(amount : BigInteger) {
        deposit(amount, true)
    }

    fun deposit(amount : BigInteger, flag:Boolean){

        walletAlice.deposit(amount)
        walletBob.deposit(amount)

        createBlocks(1)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
            }
        }

        if(flag) {
            Assert.assertEquals(amount.multiply(2.toBigInteger()), chain.getBalance(contract.contractAddress))
            Assert.assertEquals(walletAlice.balance(), amount)
            Assert.assertEquals(walletBob.balance(), amount)
        }

    }

    @Test
    fun testTransfer(){
        testDeposit()

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)
        val depositAmount = EtherUtil.convert(2000, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }

        var account=walletBob.hubAccount()

        Assert.assertEquals(account?.address,bob.address)
        Assert.assertEquals(account?.deposit,depositAmount)
        Assert.assertEquals(account?.update?.receiveAmount,amount)
        Assert.assertEquals(account?.update?.sendAmount,BigInteger.ZERO)

        account=walletAlice.hubAccount()
        Assert.assertEquals(account?.address,alice.address)
        Assert.assertEquals(account?.deposit,depositAmount)
        Assert.assertEquals(account?.update?.receiveAmount,BigInteger.ZERO)
        Assert.assertEquals(account?.update?.sendAmount,amount)

    }

    @Test
    fun testBalanceChallenge() {
        testReg()

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        walletAlice.cheat(Starcoin.HubMaliciousFlag.STEAL_DEPOSIT_VALUE)

        val amount = EtherUtil.convert(2000, EtherUtil.Unit.ETHER)
        deposit(amount,false)

        var account=walletAlice.hubAccount()
        Assert.assertEquals(account?.address,alice.address)
        Assert.assertTrue(account?.deposit?:0.toBigInteger()<amount)

        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
                println(walletAlice.getMessageChannel()?.receive())
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        createBlocks(1)

        walletAlice.deposit(amount)
        createBlocks(1)

        walletAlice.deposit(amount)
        createBlocks(1)

        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        Assert.assertTrue(contract.isRecoveryMode(alice))

    }

    @Test
    fun testFirstBalanceChallenge() {
        testReg()

        walletAlice.cheat(Starcoin.HubMaliciousFlag.STEAL_DEPOSIT_VALUE)

        val amount = EtherUtil.convert(2000, EtherUtil.Unit.ETHER)
        deposit(amount)

        var account=walletAlice.hubAccount()
        Assert.assertEquals(account?.address,alice.address)
        Assert.assertTrue(account?.deposit?:0.toBigInteger()<amount)

        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
                println(walletAlice.getMessageChannel()?.receive())
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        createBlocks(1)

        walletAlice.deposit(amount)
        createBlocks(1)

        walletAlice.deposit(amount)
        createBlocks(1)

        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        Assert.assertTrue(contract.isRecoveryMode(alice))

    }

    @Test
    fun testTransferChallenge() {
        testDeposit()

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        walletBob.cheat(Starcoin.HubMaliciousFlag.STEAL_TRANSACTION_IOU_VALUE)

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        this.walletAlice.openTransferChallenge(transaction.hash())
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        createBlocks(hubInfo.blocksPerEon)

        walletAlice.deposit(amount)
        createBlocks(1)

        walletAlice.deposit(amount)
        createBlocks(1)

        Assert.assertTrue(contract.isRecoveryMode(alice))
    }

    @Test
    fun testReg(){
        waitHubReady(stub)

        var update=walletAlice.register()
        Assert.assertNotNull(update)

        update=walletBob.register()
        Assert.assertNotNull(update)

    }

    @Test(expected = IllegalStateException::class)
    fun testWithdrawalException() {
        testDeposit()

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        walletAlice.withdrawal(amount)

    }

    @Test
    fun testWithdrawal() {
        testDeposit()

        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        walletAlice.withdrawal(amount)

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }
        Assert.assertTrue(!walletAlice.hub.hubStatus.couldWithDrawal())
        Assert.assertEquals(walletAlice.hub.getWithdrawalCoin(),amount)

        var balance=chain.getBalance(alice.address)

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        Assert.assertEquals(walletAlice.hub.getWithdrawalCoin(),BigInteger.ZERO)

        var account=walletAlice.hubAccount()
        var remaining= EtherUtil.convert(2000, EtherUtil.Unit.ETHER) - amount-amount
        Assert.assertEquals((account?.allotment?:BigInteger.ZERO)-(account?.withdraw?:BigInteger.ZERO),remaining)
        Assert.assertEquals(walletAlice.balance(),remaining)

        var balanceAfter=chain.getBalance(alice.address)

        Assert.assertTrue(balance<balanceAfter)
        Assert.assertTrue(balance+EtherUtil.convert(19, EtherUtil.Unit.ETHER)<balanceAfter)

        val balanceNoGas = balance+EtherUtil.convert(20, EtherUtil.Unit.ETHER)
        Assert.assertTrue(balanceNoGas>=balanceAfter)

    }

    @Test
    fun testCancelWithdrawal() {
        testDeposit()

        val depositAmount = EtherUtil.convert(2000, EtherUtil.Unit.ETHER)
        var amount = EtherUtil.convert(1500, EtherUtil.Unit.ETHER)
        var transaction=walletBob.hubTransfer(alice.address,amount)
        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
            }
        }

        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        Assert.assertEquals(walletAlice.hubAccount()?.balance,amount+depositAmount)
        Assert.assertEquals(walletAlice.balance(),amount+depositAmount)

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        transaction=walletAlice.hubTransfer(bob.address,amount)
        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }

        Assert.assertEquals(walletAlice.hubAccount()?.balance,depositAmount)
        Assert.assertEquals(walletAlice.balance(),depositAmount)

        amount = EtherUtil.convert(3000, EtherUtil.Unit.ETHER)
        walletAlice.hub.checkBalance=false
        walletAlice.withdrawal(amount)

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }
        Assert.assertEquals(walletAlice.hub.getWithdrawalCoin(),amount)

        createBlocks(1)

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }
        Assert.assertTrue(walletAlice.hub.hubStatus.couldWithDrawal())
        Assert.assertEquals(walletAlice.hubAccount()?.balance,depositAmount)
        Assert.assertEquals(walletBob.hubAccount()?.balance,depositAmount)

        Assert.assertEquals(walletBob.balance(),depositAmount)
        Assert.assertEquals(walletAlice.balance(),depositAmount)

        Assert.assertEquals(walletAlice.hub.getWithdrawalCoin(),BigInteger.ZERO)

    }

    @Test
    fun testSync() {
        testDeposit()

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.sync()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
    }

    @Test
    fun testSyncBytransfer() {
        testTransfer()
        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.sync()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
    }

    @Test
    @Ignore
    fun testSyncEon() {
        testDeposit()
        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.sync()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
    }

    @Test
    @Ignore
    fun testNoReg() {
        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                Assert.assertEquals(walletAlice.getMessageChannel()?.receive(), ClientEventType.FINISH_EON_CHANGE)
            }
        }

    }

    @Test
    @Ignore
    fun testRestore() {
        testDeposit()

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.restore()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
    }

    @Test
    @Ignore
    fun testNextEonRestore() {
        testDeposit()

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }
        walletAlice.hub.disconnect=true

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.restore()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
    }

    @Test(expected = IllegalStateException::class)
    fun testRestoreFail() {
        testDeposit()

        val amount=EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction=walletAlice.hubTransfer(bob.address,amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L) {
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }
        walletAlice.hub.disconnect=true

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        createBlocks(hubInfo.blocksPerEon)
        runBlocking {
            withTimeout(10000L) {
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.restore()
    }

    @Test
    @Ignore
    fun testWithdrawalRestore() {
        testDeposit()

        waitToNextEon()

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val amount = EtherUtil.convert(20, EtherUtil.Unit.ETHER)

        val transaction = walletAlice.hubTransfer(bob.address, amount)

        Assert.assertNotNull(transaction)

        runBlocking {
            withTimeout(10000L){
                walletBob.getMessageChannel()?.receive()
                walletAlice.getMessageChannel()?.receive()
                walletBob.getMessageChannel()?.receive()
            }
        }

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                println(walletAlice.getMessageChannel()?.receive())
            }
        }

        walletAlice.withdrawal(amount)

        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }
        Assert.assertTrue(!walletAlice.hub.hubStatus.couldWithDrawal())

        walletAlice.hub.disconnect=true

        waitToNextEon()
        runBlocking {
            withTimeout(10000L){
                walletAlice.getMessageChannel()?.receive()
            }
        }

        val aliceWalletClone = Wallet(chain,ClientAccount(alice,"alice"),null)
        aliceWalletClone.restore()
        Assert.assertEquals(walletAlice.balance(), aliceWalletClone.balance())
        Assert.assertEquals(walletAlice.hub.getWithdrawalCoin(), amount)

    }

    private fun waitToNextEon() {
        var height = chain.getBlockNumber()
        var blockNumber = Eon.waitToEon(hubInfo.startBlockNumber.toLong(),height,hubInfo.blocksPerEon,walletAlice.hub.currentEon.id+1)
        logger.info("current height is $height,need generate $blockNumber blocks")
        for (i in 0..blockNumber) {
            chain.createBlock()
        }
    }

    private fun createBlocks(number:Int){
        for(i in 1..number)
            chain.createBlock()
    }

}
