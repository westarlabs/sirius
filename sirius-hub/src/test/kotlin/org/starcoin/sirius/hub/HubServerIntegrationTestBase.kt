package org.starcoin.sirius.hub


import com.google.common.eventbus.Subscribe
import io.grpc.inprocess.InProcessChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.sirius.channel.receiveTimeout
import org.starcoin.sirius.core.*
import org.starcoin.sirius.eth.core.EtherUnit
import org.starcoin.sirius.eth.core.ether
import org.starcoin.sirius.eth.core.wei
import org.starcoin.sirius.protocol.*
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class HubEventFuture(private val predicate: (HubEvent) -> Boolean) : CompletableFuture<HubEvent>() {

    @Subscribe
    fun onEvent(event: HubEvent) {
        if (this.predicate(event)) {
            this.complete(event)
        }
    }
}

abstract class HubServerIntegrationTestBase<T : ChainTransaction, A : ChainAccount, C : Chain<T, out Block<T>, A>> {


    private var configuration: Configuration by Delegates.notNull()
    private var hubServer: HubServer<A> by Delegates.notNull()

    protected var chain: C by Delegates.notNull()

    private var hubService: HubService by Delegates.notNull()
    private var contract: HubContract<A> by Delegates.notNull()

    private var hubRootChannel: Channel<HubRoot> by Delegates.notNull()

    private var txMap: ConcurrentHashMap<Hash, CompletableFuture<TransactionResult<T>>> by Delegates.notNull()

    private var eon: AtomicInteger by Delegates.notNull()
    private var blockHeight: AtomicLong by Delegates.notNull()


    private var owner: A by Delegates.notNull()
    private var contractHubInfo: ContractHubInfo by Delegates.notNull()

    private var coroutineContext: CoroutineContext by Delegates.notNull()
    private var watchHubJob: Job by Delegates.notNull()
    private var watchBlockJob: Job by Delegates.notNull()
    private val localAccounts: MutableList<LocalAccount<T, A>> = mutableListOf()

    abstract fun createChainAccount(amount: Long): A
    abstract fun createChain(configuration: Configuration): C

    private fun createAndInitLocalAccount(): LocalAccount<T, A> {
        val account =
            LocalAccount(this.createChainAccount(1000), chain, contract, this.owner.key.keyPair.public, configuration)
        this.localAccounts.add(account)
        account.init()
        Assert.assertTrue(account.update.verifyHubSig(owner.key))
        return account
    }

    @Before
    @Throws(InterruptedException::class)
    fun before() {
        this.coroutineContext = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
        eon = AtomicInteger(0)

        this.txMap = ConcurrentHashMap()

        this.hubRootChannel = Channel(100)
        this.configuration = Configuration.configurationForUNIT()
        this.chain = createChain(this.configuration)

        this.owner = this.createChainAccount(10000)
        this.hubServer = HubServer(configuration, chain, this.owner)

        blockHeight = AtomicLong(this.chain.getBlockNumber())

        hubServer.start()
        contract = this.hubServer.contract
        contractHubInfo = contract.queryHubInfo(this.owner)

        this.eon.set(contractHubInfo.latestEon)

        hubService = HubServiceStub(
            HubServiceGrpc.newBlockingStub(
                InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build()
            )
        )

        this.waitServerStart()
        this.watchHubJob = this.watchEon()
        this.watchBlockJob = this.watchBlock()

        val eventch = this.chain.watchEvents(contract.contractAddress, listOf(ChainEvent.ReturnEvent))
        GlobalScope.launch {
            eventch.consumeEach { LOG.info(it.receipt.logs.toString()) }
        }
        //this.produceBlock(1)
    }

    abstract fun createBlock()

    fun produceBlock(n: Int, expectEon: Int) {
        LOG.info("produceBlock $n")
        for (i in 0..n) {
            if (eon.get() < expectEon) {
                createBlock()
            }
        }
    }

    private fun waitServerStart() {
//        while (chainService
//                .getBlocks(
//                    GetBlocksRequest.newBuilder().setHeight(0).setCount(1).setOrder(Order.DESC).build()
//                )
//                .getBlocksCount() === 0
//        ) {
//            sleep(100)
//            LOG.info("wait chain service")
//        }

        var hubInfo = hubService.hubInfo
        while (!hubInfo.isReady) {
            sleep(100)
            LOG.info("wait hub service:" + hubInfo.toString())
            //this.produceBlock(1)
            this.eon.set(hubInfo.eon)
            hubInfo = hubService.hubInfo
        }
    }

    private fun watchEon(): Job = GlobalScope.launch(this.coroutineContext) {
        val channel = hubService.watchHubRoot()
        for (hubRoot in channel) {
            LOG.info("new hubRoot: $hubRoot")
            eon.set(hubRoot.eon)
            hubRootChannel.send(hubRoot)
        }
    }

    private fun registerTxHook(txHash: Hash): CompletableFuture<TransactionResult<T>> {
        val future = CompletableFuture<TransactionResult<T>>()
        LOG.info("register tx hook:$txHash")
        val originFuture = this.txMap.putIfAbsent(txHash, future)
        if (originFuture != null) {
            future.complete(originFuture.get())
        }
        return future
    }

    private fun onTransaction(txResult: TransactionResult<T>) {
        val hash = txResult.tx.hash()
        LOG.info("onTransaction: $hash")
        val future = txMap.computeIfAbsent(hash) {
            CompletableFuture()
        }
        future.complete(txResult)
    }


    private fun watchBlock() = GlobalScope.launch(this.coroutineContext) {
        val blockChannel = chain.watchBlock()
        for (block in blockChannel) {
            LOG.info("Current blockNumber ${block.height}")
            blockHeight.set(block.height)
            //TODO tx filter
            block.transactions.forEach {
                onTransaction(it)
            }
        }
    }

    private inner class HubRootFuture(private val expectEon: Int) : CompletableFuture<HubRoot>() {

        @Subscribe
        fun onHubRoot(root: HubRoot) {
            if (expectEon <= root.eon) {
                this.complete(root)
            }
        }
    }

    private fun waitToNextEon() {
        this.waitToNextEon(true)
    }

    private fun waitToNextEon(expectSuccess: Boolean) = runBlocking {
        val expectEon = eon.get() + 1
        LOG.info("waitToNextEon:$expectEon")
        val blockCount = Eon.waitToEon(
            contractHubInfo.startBlockNumber.longValueExact(),
            blockHeight.toLong(),
            contractHubInfo.blocksPerEon,
            expectEon
        )
        //TODO FIXME
        if (blockCount <= 0) {
            return@runBlocking
        }
        produceBlock(blockCount, expectEon)
        try {
            var hubRoot = hubRootChannel.receiveTimeout()
            while (hubRoot.eon < expectEon) {
                hubRoot = hubRootChannel.receiveTimeout()
            }
            if (hubRoot.eon == expectEon) {
                verifyHubRoot(hubRoot)
            } else {
                LOG.warning("Expect eon:$expectEon, but get: ${hubRoot.eon}")
            }
            if (!expectSuccess) {
                Assert.fail("Expect fail,but success.")
            }
        } catch (e: TimeoutCancellationException) {
            if (expectSuccess) {
                Assert.fail("Expect success, but wait eon $expectEon hubRoot timeout")
            }
        }
    }


    @Test
    fun testHubService() {
        val a0 = this.createAndInitLocalAccount()
        this.waitToNextEon()
        val a1 = this.createAndInitLocalAccount()
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.deposit(a1, depositAmount)

        val transferAmount: BigInteger = 10.toBigInteger()
        val tx = this.offchainTransfer(a0, a1, transferAmount)
        Assert.assertEquals(depositAmount - transferAmount, a0.hubAccount!!.balance)
        Assert.assertEquals(depositAmount + transferAmount, a1.hubAccount!!.balance)

        this.waitToNextEon()
        Assert.assertEquals(depositAmount - transferAmount, a0.hubAccount!!.allotment)
        Assert.assertEquals(depositAmount + transferAmount, a1.hubAccount!!.allotment)

        this.transferDeliveryChallenge(a0, tx)
        this.waitToNextEon()

        withdrawal(a0, a0.hubAccount!!.balance, true)

//        this.waitToNextEon()
//        this.balanceUpdateChallenge(a0)
//        this.balanceUpdateChallenge(a1)
//        this.waitToNextEon()
    }

    @Test
    fun testBalanceUpdateChallengeWithOldUpdate() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        val depositAmount = 10.ether.inWei.value
        this.deposit(a0, depositAmount)

        this.waitToNextEon()

        val balance = a0.hubAccount!!.balance
        this.offchainTransfer(a0, a1, balance / 2.toBigInteger())

        val oldUpdate = a0.update
        this.offchainTransfer(a0, a1, balance / 2.toBigInteger())

        this.waitToNextEon()
        this.balanceUpdateChallenge(a0, oldUpdate)
        this.waitToNextEon()
    }

    @Test
    fun testBalanceUpdateChallengeWithOldUpdateAndPath() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        val depositAmount = 10.ether.inWei.value
        this.deposit(a0, depositAmount)

        this.waitToNextEon()

        val balance = a0.hubAccount!!.balance
        this.offchainTransfer(a0, a1, balance / 2.toBigInteger())

        val oldUpdate = a0.update
        this.offchainTransfer(a0, a1, balance / 2.toBigInteger())

        this.waitToNextEon()
        this.balanceUpdateChallenge(a0, BalanceUpdateProof(oldUpdate, a0.state!!.previous!!.proof!!.path))
        this.waitToNextEon()
    }


    @Test
    fun testEmptyHubRoot() {
        this.waitToNextEon()
        this.waitToNextEon()
        this.waitToNextEon()
    }

    @Test
    fun testDoNothingChallenge() {
        this.waitToNextEon()
        val a0 = this.createAndInitLocalAccount()
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a0)
        this.waitToNextEon()
    }

    @Test
    fun testInvalidWithdrawal() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        this.waitToNextEon()
        // test invalid withdrawal
        val balance = a0.hubAccount!!.balance
        this.offchainTransfer(a0, a1, balance)
        withdrawal(a0, balance, false)
        this.waitToNextEon()
    }

    @Test
    fun testNewUserBalanceUpdateChallenge() {
        val a0 = this.createAndInitLocalAccount()
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a0)
        waitToNextEon()
    }

    @Test
    fun testStealDeposit() {
        val a0 = this.createAndInitLocalAccount()
        this.waitToNextEon()
        this.hubService.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_DEPOSIT)

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, false)

        this.waitToNextEon()

        this.balanceUpdateChallenge(a0)
        waitToNextEon(false)
    }

    @Test
    fun testStealWithdrawal() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        this.hubService.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL)

        val depositAmount: BigInteger = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()
        this.waitToNextEon()

        // a0 transfer to a1, and steal back
        this.offchainTransfer(a0, a1, depositAmount)
        this.withdrawal(a0, depositAmount, true, false)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a1)
        waitToNextEon(false)
    }

    @Test
    fun testStealTx() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        this.hubService.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION)

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()
        // a1 transfer to a0, but hub not really update global state tree.
        val tx = this.offchainTransfer(a0, a1, depositAmount)
        waitToNextEon()
        Assert.assertEquals(
            depositAmount,
            this.hubService.getHubAccount(a0.address)?.allotment
        )
        Assert.assertEquals(
            0.toBigInteger(),
            this.hubService.getHubAccount(a1.address)?.allotment
        )

        this.transferDeliveryChallenge(a0, tx)
        waitToNextEon(false)
    }

    @Test
    fun testStealTxIOU() {
        val a0 = this.createAndInitLocalAccount()
        val a1 = this.createAndInitLocalAccount()
        this.hubService.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION_IOU)

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()
        // a1 transfer to a0, but hub change to other user.
        val tx = this.offchainTransfer(a0, a1, depositAmount, false)
        waitToNextEon()
        Assert.assertEquals(0.toBigInteger(), this.hubService.getHubAccount(a0.address)?.allotment)
        Assert.assertEquals(0.toBigInteger(), this.hubService.getHubAccount(a1.address)?.allotment)

        this.transferDeliveryChallenge(a0, tx)
        waitToNextEon(false)
    }

    private fun verifyHubRoot(hubRoot: HubRoot) {
        LOG.info("verifyHubRoot:" + hubRoot.eon)
        var contractRoot = contract.getLatestRoot(owner)
        Assert.assertNotNull(contractRoot)
        if (contractRoot!!.eon > hubRoot.eon) {
            return
        }
        // ensure contract and hub root is equals.
        while (hubRoot != contractRoot) {
            //TODO
            LOG.info("Wait to getLatestRoot again. localHubRoot:$hubRoot, contractRoot:$contractRoot")
            sleep(1000)
            contractRoot = contract.getLatestRoot(owner)
        }
        Assert.assertEquals(hubRoot, contractRoot)
    }

    private fun deposit(a: LocalAccount<T, A>, amount: BigInteger) {
        this.deposit(a, amount, true)
    }

    private fun deposit(a: LocalAccount<T, A>, amount: BigInteger, expectSuccess: Boolean) = runBlocking {
        val previousAccount = hubService.getHubAccount(a.address)!!
        // deposit

        val hubEventFuture =
            HubEventFuture { event -> event.type === HubEventType.NEW_DEPOSIT && event.address == a.address }
        a.watch(hubEventFuture)

        val txDeferred = chain.submitTransaction(
            a.chainAccount,
            chain.newTransaction(a.chainAccount, contract.contractAddress, amount)
        )

        val receipt = txDeferred.awaitTimout()
        Assert.assertTrue(receipt.status)

        try {
            val hubEvent = hubEventFuture.get(4, TimeUnit.SECONDS)
            if (expectSuccess) {
                Assert.assertEquals(amount, hubEvent.getPayload<Deposit>().amount)
            } else {
                Assert.fail("expect get Deposit event timeout")
            }
        } catch (e: Exception) {
            if (expectSuccess) {
                Assert.fail(e.message)
            }
        }
        //TODO ensure
        sleep(1000)
        val hubAccount = hubService.getHubAccount(a.address)!!
        Assert.assertEquals(
            if (expectSuccess) previousAccount.deposit + amount else previousAccount.deposit,
            hubAccount.deposit
        )
        a.hubAccount = hubAccount
    }

    private fun offchainTransfer(
        from: LocalAccount<T, A>,
        to: LocalAccount<T, A>,
        amount: BigInteger
    ): OffchainTransaction {
        return this.offchainTransfer(from, to, amount, true)
    }

    private fun offchainTransfer(
        from: LocalAccount<T, A>, to: LocalAccount<T, A>, amount: BigInteger, expectToReceive: Boolean
    ): OffchainTransaction {
        val fromFuture = HubEventFuture({ event -> event.type === HubEventType.NEW_UPDATE })
        val toFuture = HubEventFuture({ event -> event.type === HubEventType.NEW_UPDATE })

        from.watch(fromFuture)
        to.watch(toFuture)

        val tx = from.newTx(to.address, amount)
        tx.sign(from.key)

        val fromIOU = IOU(tx, from.update)
        hubService.sendNewTransfer(fromIOU)

        try {
            fromFuture.get(1000, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Assert.fail(e.message)
        } catch (e: ExecutionException) {
            Assert.fail(e.message)
        } catch (e: TimeoutException) {
            Assert.fail(e.message)
        }

        try {
            toFuture.get(1000, TimeUnit.MILLISECONDS)
            if (!expectToReceive) {
                Assert.fail("unexpected toUser receive event.")
            }
        } catch (e: InterruptedException) {
            if (expectToReceive) {
                Assert.fail(e.message)
            }
        } catch (e: ExecutionException) {
            if (expectToReceive) {
                Assert.fail(e.message)
            }
        } catch (e: TimeoutException) {
            if (expectToReceive) {
                Assert.fail(e.message)
            }
        }
        sleep(1000)
        Assert.assertTrue(from.update.isSignedByHub)
        if (expectToReceive) {
            Assert.assertTrue(to.update.isSignedByHub)
        }
        return tx
    }

    private fun withdrawal(account: LocalAccount<T, A>, amount: BigInteger, expectSuccess: Boolean) {
        this.withdrawal(account, amount, expectSuccess, true)
    }

    private fun withdrawal(
        account: LocalAccount<T, A>, amount: BigInteger, expectSuccess: Boolean, doCheck: Boolean
    ) = runBlocking {
        val oldHubAccount = hubService.getHubAccount(account.address)!!
        val chainBalance = chain.getBalance(account.address)
        val eon = account.state!!.eon
        val proof = account.state!!.previous!!.proof!!
        LOG.info("withdrawal: $eon, path:$proof")
        val txDeferred =
            contract.initiateWithdrawal(account.chainAccount, Withdrawal(proof, amount))
        val receipt = txDeferred.awaitTimoutOrNull()
        Assert.assertNotNull(receipt)
        //TODO
        sleep(1000)
        if (doCheck) {
            if (expectSuccess) {
                val newHubAccount = hubService.getHubAccount(account.address)!!
                Assert.assertEquals(oldHubAccount.withdraw + amount, newHubAccount.withdraw)
                waitToNextEon()
                waitToNextEon()
                val newChainBalance = chain.getBalance(account.address)
                Assert.assertTrue((chainBalance + amount).wei.fuzzyEquals(newChainBalance.wei, EtherUnit.Gwei))
            } else {
                val newHubAccount = hubService.getHubAccount(account.address)!!
                Assert.assertEquals(oldHubAccount.withdraw, newHubAccount.withdraw)
                waitToNextEon()
                waitToNextEon()
                val newChainBalance = chain.getBalance(account.address)
                Assert.assertTrue((chainBalance).wei.fuzzyEquals(newChainBalance.wei, EtherUnit.Gwei))
            }
        }
    }

    private fun balanceUpdateChallenge(account: LocalAccount<T, A>) {
        val challenge = account.state?.previous?.proof?.let { BalanceUpdateProof(it) }
            ?: BalanceUpdateProof(account.state!!.previous!!.update)
        this.balanceUpdateChallenge(account, challenge)
    }

    private fun balanceUpdateChallenge(account: LocalAccount<T, A>, update: Update) {
        this.balanceUpdateChallenge(account, BalanceUpdateProof(update))
    }

    private fun balanceUpdateChallenge(account: LocalAccount<T, A>, challenge: BalanceUpdateProof) = runBlocking {
        if (challenge.hasUpdate) {
            Assert.assertTrue(challenge.update.verifyHubSig(owner.key.keyPair.public))
        }
        val txDeferred = contract.openBalanceUpdateChallenge(account.chainAccount, challenge)
        val receipt = txDeferred.awaitTimout()
        Assert.assertTrue(receipt.status)
    }

    private fun transferDeliveryChallenge(account: LocalAccount<T, A>, offchainTx: OffchainTransaction) = runBlocking {
        val tree = MerkleTree(account.state!!.previous!!.txs)
        val path = tree.getMembershipProof(offchainTx.hash())
        Assert.assertNotNull(path)
        val challenge = TransferDeliveryChallenge(account.state!!.previous!!.update, offchainTx, path)
        val txDeferred = contract.openTransferDeliveryChallenge(account.chainAccount, challenge)
        val receipt = txDeferred.awaitTimoutOrNull()
        Assert.assertTrue(receipt!!.status)
    }

    @After
    fun after() {
        for (a in this.localAccounts) {
            a.destroy()
        }
        this.localAccounts.clear()
        this.watchHubJob.cancel()
        this.hubService.resetHubMaliciousFlag()
        this.hubServer.stop()
        this.watchBlockJob.cancel()
        this.chain.stop()
    }

    companion object : WithLogging() {

        private fun sleep(time: Long) {
            try {
                Thread.sleep(time)
            } catch (e: InterruptedException) {
            }

        }
    }
}
