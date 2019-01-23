package org.starcoin.sirius.hub


import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.protobuf.Empty
import io.grpc.inprocess.InProcessChannelBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ethereum.db.IndexedBlockStore
import org.ethereum.util.blockchain.EtherUtil
import org.junit.*
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates

class HubEventFuture(private val predicate: (HubEvent) -> Boolean) : CompletableFuture<HubEvent>() {

    @Subscribe
    fun onEvent(event: HubEvent) {
        if (this.predicate(event)) {
            this.complete(event)
        }
    }
}

class BlockFuture : CompletableFuture<IndexedBlockStore.BlockInfo>() {

    @Subscribe
    fun onBlock(block: IndexedBlockStore.BlockInfo) {
        this.complete(block)
    }
}


abstract class HubServerIntegrationTestBase<T : ChainTransaction, A : ChainAccount> {


    var configuration: Configuration by Delegates.notNull()
    var hubServer: HubServer<T, A> by Delegates.notNull()

    abstract val chain: Chain<T, out Block<T>, A>

    var hubService: HubServiceGrpc.HubServiceBlockingStub by Delegates.notNull()
    var contract: HubContract<A> by Delegates.notNull()
    internal var eventBus: EventBus by Delegates.notNull()

    internal var txMap: ConcurrentHashMap<Hash, CompletableFuture<TransactionResult<T>>> by Delegates.notNull()

    internal var eon = AtomicInteger(0)
    internal var blockHeight = AtomicLong(0)


    internal var a0: LocalAccount<A> by Delegates.notNull()
    internal var a1: LocalAccount<A> by Delegates.notNull()

    abstract fun createChainAccount(amount: Long): A
    var owner: A by Delegates.notNull()
    var contractHubInfo: ContractHubInfo by Delegates.notNull()

    @Before
    @Throws(InterruptedException::class)
    fun before() {
        this.txMap = ConcurrentHashMap()

        this.eventBus = EventBus()
        this.configuration = Configuration.configurationForUNIT()
        this.owner = this.createChainAccount(10000)
        this.hubServer = HubServer(configuration, chain, owner)

        hubServer.start()
        contract = this.hubServer.contract
        contractHubInfo = contract.queryHubInfo(this.owner)
        this.eon.set(contractHubInfo.latestEon)

        hubService = HubServiceGrpc.newBlockingStub(
            InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build()
        )
        this.a0 = LocalAccount(this.createChainAccount(1000), hubService)
        this.a1 = LocalAccount(this.createChainAccount(1000), hubService)

        this.waitServerStart()
        this.watchEon()
        this.watchBlock()
        //this.produceBlock(1)
    }

    abstract fun createBlock()

    fun produceBlock(n: Int) {
        for (i in 0..n) {
            createBlock()
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

        var hubInfo = hubService.getHubInfo(Empty.newBuilder().build())
        while (!hubInfo.getReady()) {
            sleep(100)
            LOG.info("wait hub service:" + hubInfo.toString())
            this.produceBlock(1)
            this.eon.set(hubInfo.eon)
            hubInfo = hubService.getHubInfo(Empty.newBuilder().build())
        }
    }

    private fun watchEon() {
        GlobalScope.launch {
            try {
                hubService
                    .watchHubRoot(Empty.newBuilder().build())
                    .forEachRemaining { protoHubRoot ->
                        val hubRoot = HubRoot.parseFromProtoMessage(protoHubRoot)
                        LOG.info("new hubRoot: $hubRoot")
                        eon.set(hubRoot.eon)
                        eventBus.post(hubRoot)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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


    private fun watchBlock() {
        GlobalScope.launch {
            val blockChannel = chain.watchBlock()
            for (block in blockChannel) {
                blockHeight.set(block.height)
                eventBus.post(block)
            }
        }

        GlobalScope.launch {
            //TODO add filter
            val txChannel = chain.watchTransactions()
            for (txResult in txChannel) {
                onTransaction(txResult)
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

    private fun waitToNextEon(expectSuccess: Boolean) {
        val expectEon = this.eon.get() + 1
        LOG.info("waitToNextEon:$expectEon")
        val future = HubRootFuture(expectEon)
        this.eventBus.register(future)
        this.produceBlock(
            Eon.waitToEon(
                contractHubInfo.startBlockNumber.longValueExact(),
                blockHeight.toLong(),
                contractHubInfo.blocksPerEon,
                expectEon
            )
        )
        try {
            val hubRoot = future.get(1000, TimeUnit.MILLISECONDS)
            this.eon.set(hubRoot.eon)
            if (expectSuccess) {
                Assert.assertEquals(expectEon.toLong(), this.eon.get().toLong())
            }
            this.verifyHubRoot(hubRoot)
            if (this.a0.isRegister) {
                this.a0.onEon(hubRoot)
            }
            if (this.a1.isRegister) {
                this.a1.onEon(hubRoot)
            }
            if (!expectSuccess) {
                Assert.fail("expect waitToNextEon fail, but success.")
            }
        } catch (e: InterruptedException) {
            if (expectSuccess) {
                e.printStackTrace()
                Assert.fail(e.message)
            }
        } catch (e: ExecutionException) {
            if (expectSuccess) {
                e.printStackTrace()
                Assert.fail(e.message)
            }
        } catch (e: TimeoutException) {
            if (expectSuccess) {
                e.printStackTrace()
                Assert.fail(e.message)
            }
        }

    }


    @Test
    fun testHubService() {
        register(eon.get(), a0)
        this.waitToNextEon()
        register(eon.get(), a1)

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

        this.waitToNextEon()
        this.balanceUpdateChallenge(a0)
        this.balanceUpdateChallenge(a1)
        this.waitToNextEon()
    }

    @Ignore
    @Test
    fun testBalanceUpdateChallengeWithOldUpdate() {
        register(eon.get(), a0)
        register(eon.get(), a1)
        val depositAmount = 100.toBigInteger()
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

    @Ignore
    @Test
    fun testEmptyHubRoot() {
        this.waitToNextEon()
        this.waitToNextEon()
        this.waitToNextEon()
    }

    @Ignore
    @Test
    fun testDoNothingChallenge() {
        this.waitToNextEon()
        register(eon.get(), a0)
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a0)
        this.waitToNextEon()
    }

    @Ignore
    @Test
    fun testInvalidWithdrawal() {
        register(eon.get(), a0)
        register(eon.get(), a1)
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        // test invalid withdrawal
        val balance = a0.hubAccount!!.balance
        this.offchainTransfer(a0, a1, balance)
        withdrawal(a0, balance, false)
        this.waitToNextEon()
    }

    @Ignore
    @Test
    fun testNewUserBalanceUpdateChallenge() {
        register(eon.get(), a0)
        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a0)
        waitToNextEon()
    }

    @Ignore
    @Test
    fun testStealDeposit() {
        register(eon.get(), a0)
        this.waitToNextEon()
        this.hubService.setMaliciousFlags(
            Hub.HubMaliciousFlag.toProto(EnumSet.of(Hub.HubMaliciousFlag.STEAL_DEPOSIT))
        )

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, false)

        this.waitToNextEon()

        this.balanceUpdateChallenge(a0)
        waitToNextEon(false)
    }

    @Ignore
    @Test
    fun testStealWithdrawal() {
        register(eon.get(), a0)
        register(eon.get(), a1)
        this.hubService.setMaliciousFlags(
            Hub.HubMaliciousFlag.toProto(EnumSet.of(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL))
        )

        val depositAmount: BigInteger = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()

        // a0 transfer to a1, and steal back
        this.offchainTransfer(a0, a1, depositAmount)
        this.withdrawal(a0, depositAmount, true, false)
        this.waitToNextEon()
        this.balanceUpdateChallenge(a1)
        waitToNextEon(false)
    }

    @Ignore
    @Test
    fun testStealTx() {
        register(eon.get(), a0)
        register(eon.get(), a1)
        this.hubService.setMaliciousFlags(
            Hub.HubMaliciousFlag.toProto(EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION))
        )

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()
        // a1 transfer to a0, but hub not really update global state tree.
        val tx = this.offchainTransfer(a0, a1, depositAmount)
        waitToNextEon()
        Assert.assertEquals(
            depositAmount, this.hubService.getHubAccount(a0.address.toProto()).allotment
        )
        Assert.assertEquals(0, this.hubService.getHubAccount(a1.address.toProto()).allotment)

        this.transferDeliveryChallenge(a0, tx)
        waitToNextEon(false)
    }

    @Ignore
    @Test
    fun testStealTxIOU() {
        register(eon.get(), a0)
        register(eon.get(), a1)
        this.hubService.setMaliciousFlags(
            Hub.HubMaliciousFlag.toProto(EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION_IOU))
        )

        val depositAmount = 100.toBigInteger()
        this.deposit(a0, depositAmount, true)
        this.waitToNextEon()
        // a1 transfer to a0, but hub change to other user.
        val tx = this.offchainTransfer(a0, a1, depositAmount, false)
        waitToNextEon()
        Assert.assertEquals(0, this.hubService.getHubAccount(a0.address.toProto()).allotment)
        Assert.assertEquals(0, this.hubService.getHubAccount(a1.address.toProto()).allotment)

        this.transferDeliveryChallenge(a0, tx)
        waitToNextEon(false)
    }

    private fun verifyHubRoot(hubRoot: HubRoot) {
        LOG.info("verifyHubRoot:" + hubRoot.eon)
        var contractRoot = contract.getLatestRoot(owner)
        Assert.assertNotNull(contractRoot)
        // ensure contract and hub root is equals.
        while (hubRoot != contractRoot) {
            //TODO
            println("Wait to getLatestRoot again.")
            sleep(1000)
            contractRoot = contract.getLatestRoot(owner)
        }
        Assert.assertEquals(hubRoot, contractRoot)
    }

    private fun register(eon: Int, a: LocalAccount<A>) {
        val initUpdate = a.initUpdate(eon)

        // register
        val updateV0 = Update.parseFromProtoMessage(
            hubService.registerParticipant(
                Starcoin.RegisterParticipantRequest.newBuilder()
                    .setParticipant(a.p.toProto<Starcoin.Participant>())
                    .setUpdate(initUpdate.toProto<Starcoin.Update>())
                    .build()
            )
        )
        a.register(updateV0, HubAccount.parseFromProtoMessage(this.hubService.getHubAccount(a.address.toProto())))
    }

    private fun deposit(a: LocalAccount<A>, amount: BigInteger) {
        this.deposit(a, amount, true)
    }

    private fun deposit(a: LocalAccount<A>, amount: Long) {
        this.deposit(a, amount.toBigInteger())
    }

    private fun deposit(a: LocalAccount<A>, amount: BigInteger, expectSuccess: Boolean) {
        val previousAccount = HubAccount.parseFromProtoMessage(hubService.getHubAccount(a.address.toProto()))
        // deposit

        val hubEventFuture =
            HubEventFuture { event -> event.type === HubEventType.NEW_DEPOSIT && event.address == a.address }
        a.watch(hubEventFuture)

        val txHash = chain.submitTransaction(
            a.chainAccount,
            chain.newTransaction(a.chainAccount, contract.contractAddress, amount)
        )

        val future = this.registerTxHook(txHash)
        future.get(4, TimeUnit.SECONDS)

        try {
            val hubEvent = hubEventFuture.get(2, TimeUnit.SECONDS)
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
        val hubAccount = HubAccount.parseFromProtoMessage(hubService.getHubAccount(a.address.toProto()))
        Assert.assertEquals(
            if (expectSuccess) previousAccount.deposit + amount else previousAccount.deposit,
            hubAccount.deposit
        )
        a.hubAccount = hubAccount
    }

    private fun offchainTransfer(from: LocalAccount<A>, to: LocalAccount<A>, amount: BigInteger): OffchainTransaction {
        return this.offchainTransfer(from, to, amount, true)
    }

    private fun offchainTransfer(
        from: LocalAccount<A>, to: LocalAccount<A>, amount: BigInteger, expectToReceive: Boolean
    ): OffchainTransaction {
        val fromFuture = HubEventFuture({ event -> event.type === HubEventType.NEW_UPDATE })
        val toFuture = HubEventFuture({ event -> event.type === HubEventType.NEW_UPDATE })

        from.watch(fromFuture)
        to.watch(toFuture)

        val tx = from.newTx(to.address, amount)
        tx.sign(from.kp)

        val fromIOU = IOU(tx, from.update)
        Assert.assertTrue(hubService.sendNewTransfer(fromIOU.toProto()).getSucc())

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

        Assert.assertTrue(from.update.isSignedByHub)
        if (expectToReceive) {
            Assert.assertTrue(to.update.isSignedByHub)
        }
        return tx
    }

    private fun withdrawal(account: LocalAccount<A>, amount: BigInteger, expectSuccess: Boolean) {
        this.withdrawal(account, amount, expectSuccess, true)
    }

    private fun withdrawal(
        account: LocalAccount<A>, amount: BigInteger, expectSuccess: Boolean, doCheck: Boolean
    ) {
        val oldHubAccount = HubAccount.parseFromProtoMessage(this.hubService.getHubAccount(account.address.toProto()))
        val chainBalance = this.chain.getBalance(account.address)
        val eon = account.state!!.eon
        val proof = account.state!!.previous!!.proof!!
        LOG.info("withdrawal: $eon, path:$proof")
        val txHash =
            this.contract.initiateWithdrawal(account.chainAccount, Withdrawal(proof, amount))
        val future = this.registerTxHook(txHash)
        try {
            Assert.assertTrue(future.get(3, TimeUnit.SECONDS).receipt.status)
        } catch (e: InterruptedException) {
            Assert.fail(e.message)
        } catch (e: ExecutionException) {
            Assert.fail(e.message)
        } catch (e: TimeoutException) {
            Assert.fail(e.message)
        }
        //TODO
        sleep(1000)
        if (doCheck) {
            if (expectSuccess) {
                val newHubAccount =
                    HubAccount.parseFromProtoMessage(this.hubService.getHubAccount(account.address.toProto()))
                Assert.assertEquals(oldHubAccount.withdraw + amount, newHubAccount.withdraw)
                waitToNextEon()
                waitToNextEon()
                val newChainBalance = this.chain.getBalance(account.address)
                Assert.assertTrue(chainBalance + amount - newChainBalance < EtherUtil.convert(1, EtherUtil.Unit.ETHER))
            } else {
                val newHubAccount =
                    HubAccount.parseFromProtoMessage(this.hubService.getHubAccount(account.address.toProto()))
                Assert.assertEquals(oldHubAccount.withdraw, newHubAccount.withdraw)
                waitToNextEon()
                waitToNextEon()
                val newChainBalance = this.chain.getBalance(account.address)
                Assert.assertEquals(chainBalance, newChainBalance)
            }
        }
    }

    private fun balanceUpdateChallenge(account: LocalAccount<A>) {
        this.balanceUpdateChallenge(account, account.state!!.previous!!.update)
    }

    private fun balanceUpdateChallenge(account: LocalAccount<A>, update: Update) {
        //TODO
        val challenge = BalanceUpdateProof(account.state!!.previous!!.proof!!)

        val txHash = contract.openBalanceUpdateChallenge(account.chainAccount, challenge)
        val future = this.registerTxHook(txHash)
        try {
            Assert.assertTrue(
                future.get(2, TimeUnit.SECONDS).receipt.status
            )
        } catch (e: InterruptedException) {
            e.printStackTrace()
            LOG.severe("get tx " + txHash.toString() + " timeout.")
            Assert.fail(e.message)
        } catch (e: ExecutionException) {
            e.printStackTrace()
            LOG.severe("get tx " + txHash.toString() + " timeout.")
            Assert.fail(e.message)
        } catch (e: TimeoutException) {
            e.printStackTrace()
            LOG.severe("get tx " + txHash.toString() + " timeout.")
            Assert.fail(e.message)
        }

    }

    private fun transferDeliveryChallenge(account: LocalAccount<A>, offchainTx: OffchainTransaction) {
        val tree = MerkleTree(account.state!!.previous!!.txs)
        val path = tree.getMembershipProof(offchainTx.hash())
        Assert.assertNotNull(path)
        val challenge = TransferDeliveryChallenge(account.state!!.previous!!.update, offchainTx, path)
        val txHash = contract.openTransferDeliveryChallenge(account.chainAccount, challenge)
        val future = this.registerTxHook(txHash)
        try {
            Assert.assertTrue(
                future.get(2, TimeUnit.SECONDS).receipt.status
            )
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Assert.fail(e.message)
        } catch (e: ExecutionException) {
            e.printStackTrace()
            Assert.fail(e.message)
        } catch (e: TimeoutException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }

    }

    @After
    fun after() {
        this.hubService.resetMaliciousFlags(Empty.newBuilder().build())
        this.hubServer.stop()
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
