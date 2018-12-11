package org.starcoin.sirius.hub

import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.proto.Starcoin.InitiateWithdrawalRequest
import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.AugmentedMerkleTree.AugmentedMerkleTreeNode
import org.starcoin.sirius.hub.Hub.MaliciousFlag
import org.starcoin.sirius.util.KeyPairUtil
import java.security.KeyPair
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

class HubTest {

    private val logger = Logger.getLogger(HubTest::class.java.name)

    private var hub: HubImpl? = null
    private var txs: MutableList<ChainTransaction> = ArrayList()
    private var listenerReference: AtomicReference<(BlockInfo) -> Unit>? = null
    private var blockHeight = AtomicInteger()

    //internal var globalBalance: GlobalBalance

    private var root: AugmentedMerkleTreeNode? = null

    private var totalHubBalance = AtomicLong()

    private var a0: LocalAccount? = null
    private var a1: LocalAccount? = null

    //internal var configuration: Configuration
    private val blocksPerEon = 4

    @Before
    fun before() {
        a0 = LocalAccount()
        a1 = LocalAccount()
        //this.configuration = Configuration.configurationForUNIT()

        val hubKeyPair = KeyPairUtil.generateKeyPair()
        //globalBalance = GlobalBalance(hubKeyPair.getPublic())

        listenerReference = AtomicReference()
        val connection = object : HubChainConnection {
            override fun watchBlock(blockInfoListener: (BlockInfo) -> Unit) {
                listenerReference!!.set(blockInfoListener)
            }

            override fun submitTransaction(transaction: ChainTransaction) {
                txs.add(transaction)
            }
        }

        hub = HubImpl(hubKeyPair, blocksPerEon, connection)
        hub!!.start()
        newBlock()
        waitHubReady()
    }

    private fun processTransaction(tx: ChainTransaction) {
        if (tx.action == null && tx.to == Constants.CONTRACT_ADDRESS) {
            //TODO
            //globalBalance.deposit(Deposit(tx.getFrom(), tx.getAmount()))
        } else if (tx.action != null) {
            if (tx.action == "Commit") {
                //TODO
//                val hubRoot = sirius.coreContractServiceGrpc.getCommitMethod()
//                    .getRequestMarshaller()
//                    .parse(ByteArrayInputStream(tx.getArguments()))
//                val result = this.globalBalance.commit(HubRoot(hubRoot))
//                Assert.assertTrue(result)
                tx.receipt = Receipt(true)
            } else if (tx.action == "InitiateWithdrawal") {
                //TODO
//                val request = sirius.coreContractServiceGrpc.getInitiateWithdrawalMethod()
//                    .parseRequest(ByteArrayInputStream(tx.getArguments()))
//                val withdrawal = Withdrawal(request)
//                this.globalBalance.withdrawal(withdrawal.getAddress(), WithdrawalStatus(withdrawal))
            }
        }
    }

    private inner class LocalAccount {

        val kp: KeyPair = KeyPairUtil.generateKeyPair()
        var p: Participant
        var update: Update? = null
        var hubAccount: HubAccount? = null

        val address: BlockAddress
            get() = this.p.address!!

        val isRegister: Boolean
            get() = this.hubAccount != null

        init {
            this.p = Participant(kp.public)
        }

        fun initUpdate(eon: Int): Update {
            update = Update(eon, 0, 0, 0, null)
            update!!.sign(kp.private)
            return update!!
        }
    }

    @Test
    fun testHub() {
        val eon = hub!!.currentEon()!!.id
        this.root = hub!!.stateRoot

        this.register(a0!!)
        // test only one user.
        goToNextEon()
        verifyProof(a0!!)

        this.register(a1!!)

        // deposit
        val amount = RandomUtils.nextLong(10, 100000)
        this.deposit(a0!!, amount)
        this.deposit(a1!!, amount)

        // transfer
        this.transfer(eon, a0!!, a1!!, amount)

        goToNextEon()

        val eon1 = hub!!.currentEon()!!.id
        Assert.assertTrue(eon < eon1)

        this.verifyRoot()

        this.withdraw(a1!!.address, RandomUtils.nextLong(1, a1!!.hubAccount!!.balance))

        goToNextEon()
        this.verifyRoot()

        this.randomEon(a0!!, a1!!)
    }

    @Test
    fun testSetMaliciousFlags() {
        hub!!.hubMaliciousFlag = EnumSet.of(MaliciousFlag.STEAL_DEPOSIT)

        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_DEPOSIT))

        hub!!.hubMaliciousFlag = EnumSet.of(MaliciousFlag.STEAL_WITHDRAWAL)
        hub!!.hubMaliciousFlag = EnumSet.of(MaliciousFlag.STEAL_TRANSACTION)

        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_DEPOSIT))
        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_WITHDRAWAL))
        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_TRANSACTION))

        hub!!.resetHubMaliciousFlag()

        Assert.assertFalse(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_DEPOSIT))
        Assert.assertFalse(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_WITHDRAWAL))
        Assert.assertFalse(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_TRANSACTION))
    }

    @Test
    fun testMaliciousStealDeposit() {
        this.register(a0!!)
        hub!!.hubMaliciousFlag = EnumSet.of(MaliciousFlag.STEAL_DEPOSIT)
        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_DEPOSIT))
        // deposit
        val amount = RandomUtils.nextLong(10, 100000)
        this.deposit(a0!!, amount, false)
        goToNextEon()
        this.verifyRoot()
    }

    @Test
    fun testMaliciousStealTx() {
        this.register(a0!!)
        this.register(a1!!)
        hub!!.hubMaliciousFlag = EnumSet.of(MaliciousFlag.STEAL_TRANSACTION)
        Assert.assertTrue(hub!!.hubMaliciousFlag.contains(MaliciousFlag.STEAL_TRANSACTION))
        // deposit
        val amount = RandomUtils.nextLong(10, 100000)
        this.deposit(a0!!, amount, true)
        val fromBalance = hub!!.getHubAccount(a0!!.address)!!.balance
        val toBalance = hub!!.getHubAccount(a1!!.address)!!.balance

        this.transfer(hub!!.currentEon()!!.id, a0!!, a1!!, amount)

        Assert.assertEquals(fromBalance, hub!!.getHubAccount(a0!!.address)!!.balance)
        Assert.assertEquals(toBalance, hub!!.getHubAccount(a1!!.address)!!.balance)
        goToNextEon()
        this.verifyRoot()
    }

    private fun randomEon(a0: LocalAccount, a1: LocalAccount) {
        val eons = RandomUtils.nextInt(3, 10)
        for (i in 0 until eons) {
            val random = RandomUtils.nextBoolean()
            val from = if (random) a0 else a1
            val to = if (random) a1 else a0
            if (from.hubAccount!!.balance == 0.toLong()) {
                continue
            }
            this.transfer(
                hub!!.currentEon()!!.id, from, to, RandomUtils.nextLong(1, from.hubAccount!!.balance)
            )
            goToNextEon()
        }
    }

    private fun register(account: LocalAccount) {
        val eon = hub!!.currentEon()!!.id
        account.initUpdate(eon)
        account.update = hub!!.registerParticipant(account.p, account.update!!)
        account.hubAccount = hub!!.getHubAccount(account.address)
    }

    private fun deposit(account: LocalAccount, amount: Long) {
        this.deposit(account, amount, true)
    }

    private fun deposit(account: LocalAccount, amount: Long, expectSuccess: Boolean) {
        this.txs.add(ChainTransaction(account.address, Constants.CONTRACT_ADDRESS, amount))
        val previousDeposit = hub!!.getHubAccount(account.address)!!.deposit
        newBlock()
        account.hubAccount = hub!!.getHubAccount(account.address)
        if (expectSuccess) {
            Assert.assertEquals(account.hubAccount!!.deposit, previousDeposit + amount)
        } else {
            Assert.assertEquals(account.hubAccount!!.deposit, previousDeposit)
        }
        totalHubBalance.addAndGet(amount)
    }

    private fun transfer(eon: Int, from: LocalAccount, to: LocalAccount, amount: Long) {
        val tx = OffchainTransaction(eon, from.address, to.address, amount)
        val fromTxs = ArrayList(from.hubAccount!!.getTransactions())
        fromTxs.add(tx)

        val fromUpdate = Update(eon, from.update!!.version + 1, from.address, fromTxs)
        fromUpdate.sign(from.kp.private)

        val toTxs = ArrayList(to.hubAccount!!.getTransactions())
        toTxs.add(tx)

        val toUpdate = Update(eon, to.update!!.version + 1, to.address, toTxs)
        toUpdate.sign(to.kp.private)

        val updates = hub!!.transfer(tx, fromUpdate, toUpdate)
        from.update = updates[0]
        to.update = updates[1]
    }

    private fun withdraw(address: BlockAddress, amount: Long) {
        this.txs.add(
            ChainTransaction(
                address,
                Constants.CONTRACT_ADDRESS,
                "InitiateWithdrawal",
                //sirius.coreContractServiceGrpc.getInitiateWithdrawalMethod().getFullMethodName(),
                InitiateWithdrawalRequest.newBuilder()
                    .setAddress(address.toByteString())
                    .setAmount(amount)
                    .setPath(hub!!.getProof(address)!!.toProto())
                    .build()
            )
        )
        newBlock()
        Assert.assertEquals(amount, hub!!.getHubAccount(address)!!.withdraw)
        totalHubBalance.addAndGet(-amount)
    }

    private fun newBlock(): BlockInfo {
        val blockInfo = BlockInfo(blockHeight.getAndIncrement())
        if (this.txs.size > 0) {
            this.txs.stream().peek { this.processTransaction(it) }
                .forEach { blockInfo.addTransaction(it) }
            this.txs = ArrayList()
        }
        listenerReference!!.get()(blockInfo)
        return blockInfo
    }

    private fun waitHubReady() {
        val queue = hub!!.watchByFilter { event -> event.type === HubEventType.NEW_HUB_ROOT }
        // generate new block to return hub commit result, and trigger hub ready.
        newBlock()
        try {
            logger.info("waitHubReady")
            queue.take()
        } catch (e: InterruptedException) {
            logger.severe(e.message)
        }

    }

    private fun goToNextEon() {
        val expectEon = this.hub!!.currentEon()!!.id + 1
        val queue = hub!!.watchByFilter { event -> event.type === HubEventType.NEW_HUB_ROOT }
        var event: HubEvent<HubRoot>? = queue.poll() as HubEvent<HubRoot>?
        while (event == null || event.payload!!.eon < expectEon) {
            this.newBlock()
            event = queue.poll() as HubEvent<HubRoot>?
        }
        Assert.assertEquals(expectEon, event!!.payload!!.eon)
        this.verifyRoot()
    }

    private fun verifyRoot() {
        val newRoot = hub!!.stateRoot
        //TODO
        //Assert.assertEquals(this.globalBalance.getRoot().hubRoot2AugmentedMerkleTreeNode(), newRoot)
        Assert.assertEquals(totalHubBalance.get(), newRoot.allotment)
        this.root = newRoot
        if (this.a0!!.isRegister) {
            this.verifyProof(a0!!)
        }
        if (this.a1!!.isRegister) {
            this.verifyProof(a1!!)
        }
    }

    private fun verifyProof(account: LocalAccount) {
        val proof = hub!!.getProof(account.address)!!
        val proof1 = hub!!.getProof(hub!!.currentEon()!!.id, account.address)
        Assert.assertEquals(proof, proof1)

        Assert.assertNotNull(proof)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(root!!, proof))
        account.hubAccount = hub!!.getHubAccount(account.address)
    }
}
