package org.starcoin.sirius.hub

import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.Delegates

abstract class HubTestBase<T:ChainTransaction, A:ChainAccount> {
    
    companion object :WithLogging()


    private lateinit var hub: HubImpl<T, A>
    private var txs: MutableList<ChainTransaction> = ArrayList()
    private var listenerReference: AtomicReference<(Block<*>) -> Unit>? = null
    private var blockHeight = AtomicInteger()

    //internal var globalBalance: GlobalBalance

    private var root: AMTreeNode? = null

    private var totalHubBalance = AtomicLong()

    private var a0: LocalAccount by Delegates.notNull()
    private var a1: LocalAccount by Delegates.notNull()

    //internal var configuration: Configuration
    private val blocksPerEon = 4

    abstract fun createChainAccount(): A

    abstract fun createHubImpl(): HubImpl<T, A>

    @Before
    fun before() {
        a0 = LocalAccount()
        a1 = LocalAccount()
        //this.configuration = Configuration.configurationForUNIT()
        val owner = this.createChainAccount()
        //globalBalance = GlobalBalance(hubKeyPair.getPublic())

        listenerReference = AtomicReference()
        val connection = object : HubChainConnection {
            override fun watchBlock(blockInfoListener: (Block<*>) -> Unit) {
                listenerReference!!.set(blockInfoListener)
            }

            override fun submitTransaction(transaction: ChainTransaction) {
                txs.add(transaction)
            }
        }

        hub = this.createHubImpl()
        hub.start()
        newBlock()
        waitHubReady()
    }

    private fun processTransaction(tx: ChainTransaction) {
//        if (tx.action == null && tx.to == Constants.CONTRACT_ADDRESS) {
//            //TODO
//            //globalBalance.deposit(Deposit(tx.getFrom(), tx.getAmount()))
//        } else if (tx.action != null) {
//            if (tx.action == "Commit") {
//                //TODO
////                val hubRoot = sirius.coreContractServiceGrpc.getCommitMethod()
////                    .getRequestMarshaller()
////                    .parse(ByteArrayInputStream(tx.getArguments()))
////                val result = this.globalBalance.commit(HubRoot(hubRoot))
////                Assert.assertTrue(result)
//                tx.receipt = Receipt(true)
//            } else if (tx.action == "InitiateWithdrawal") {
//                //TODO
////                val request = sirius.coreContractServiceGrpc.getInitiateWithdrawalMethod()
////                    .parseRequest(ByteArrayInputStream(tx.getArguments()))
////                val withdrawal = Withdrawal(request)
////                this.globalBalance.withdrawal(withdrawal.getAddress(), WithdrawalStatus(withdrawal))
//            }
//        }
    }

    private inner class LocalAccount {
        val chainAccount = createChainAccount()
        val kp: CryptoKey = chainAccount.key
        var p: Participant
        lateinit var update: Update
        var hubAccount: HubAccount? = null

        val address: Address
            get() = this.p.address

        val isRegister: Boolean
            get() = this.hubAccount != null

        init {
            this.p = Participant(kp.keyPair.public)
        }

        fun initUpdate(eon: Int): Update {
            update = Update(eon, 0, 0, 0)
            update.sign(kp)
            return update
        }
    }

    @Test
    fun testHub() {
        val eon = hub.currentEon().id
        this.root = hub.stateRoot

        this.register(a0)
        // test only one user.
        goToNextEon()
        verifyProof(a0)

        this.register(a1)

        // deposit
        val amount = RandomUtils.nextLong(10, 100000).toBigInteger()
        this.deposit(a0, amount)
        this.deposit(a1, amount)

        // transfer
        this.transfer(eon, a0, a1, amount)

        goToNextEon()

        val eon1 = hub.currentEon().id
        Assert.assertTrue(eon < eon1)

        this.verifyRoot()

        this.withdraw(a1.address, RandomUtils.nextLong(1, a1.hubAccount!!.balance.longValueExact()))

        goToNextEon()
        this.verifyRoot()

        this.randomEon(a0, a1)
    }

    @Test
    fun testSetMaliciousFlags() {
        hub.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_DEPOSIT)

        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_DEPOSIT))

        hub.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL)
        hub.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION)

        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_DEPOSIT))
        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL))
        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION))

        hub.resetHubMaliciousFlag()

        Assert.assertFalse(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_DEPOSIT))
        Assert.assertFalse(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL))
        Assert.assertFalse(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION))
    }

    @Test
    fun testMaliciousStealDeposit() {
        this.register(a0)
        hub.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_DEPOSIT)
        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_DEPOSIT))
        // deposit
        val amount = RandomUtils.nextLong(10, 100000).toBigInteger()
        this.deposit(a0, amount, false)
        goToNextEon()
        this.verifyRoot()
    }

    @Test
    fun testMaliciousStealTx() {
        this.register(a0)
        this.register(a1)
        hub.hubMaliciousFlag = EnumSet.of(Hub.HubMaliciousFlag.STEAL_TRANSACTION)
        Assert.assertTrue(hub.hubMaliciousFlag.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION))
        // deposit
        val amount = RandomUtils.nextLong(10, 100000).toBigInteger()
        this.deposit(a0, amount, true)
        val fromBalance = hub.getHubAccount(a0.address)!!.balance
        val toBalance = hub.getHubAccount(a1.address)!!.balance

        this.transfer(hub.currentEon().id, a0, a1, amount)

        Assert.assertEquals(fromBalance, hub.getHubAccount(a0.address)!!.balance)
        Assert.assertEquals(toBalance, hub.getHubAccount(a1.address)!!.balance)
        goToNextEon()
        this.verifyRoot()
    }

    private fun randomEon(a0: LocalAccount, a1: LocalAccount) {
        val eons = RandomUtils.nextInt(3, 10)
        for (i in 0 until eons) {
            val random = RandomUtils.nextBoolean()
            val from = if (random) a0 else a1
            val to = if (random) a1 else a0
            if (from.hubAccount!!.balance == BigInteger.ZERO) {
                continue
            }
            this.transfer(
                hub.currentEon().id,
                from,
                to,
                RandomUtils.nextLong(1, from.hubAccount!!.balance.longValueExact()).toBigInteger()
            )
            goToNextEon()
        }
    }

    private fun register(account: LocalAccount) {
        val eon = hub.currentEon().id
        account.initUpdate(eon)
        account.update = hub.registerParticipant(account.p, account.update)
        account.hubAccount = hub.getHubAccount(account.address)
    }

    private fun deposit(account: LocalAccount, amount: BigInteger) {
        this.deposit(account, amount, true)
    }

    private fun deposit(account: LocalAccount, amount: BigInteger, expectSuccess: Boolean) {
        //this.txs.add(ChainTransaction(account.address, Constants.CONTRACT_ADDRESS, amount))
        val previousDeposit = hub.getHubAccount(account.address)!!.deposit
        newBlock()
        account.hubAccount = hub.getHubAccount(account.address)
        if (expectSuccess) {
            Assert.assertEquals(account.hubAccount!!.deposit, previousDeposit + amount)
        } else {
            Assert.assertEquals(account.hubAccount!!.deposit, previousDeposit)
        }
        totalHubBalance.addAndGet(amount.longValueExact())
    }

    private fun transfer(eon: Int, from: LocalAccount, to: LocalAccount, amount: BigInteger) {
        val tx = OffchainTransaction(eon, from.address, to.address, amount)
        val fromTxs = ArrayList(from.hubAccount!!.getTransactions())
        fromTxs.add(tx)

        val fromUpdate = Update.newUpdate(eon, from.update.version + 1, from.address, fromTxs)
        fromUpdate.sign(from.kp)

        val toTxs = ArrayList(to.hubAccount!!.getTransactions())
        toTxs.add(tx)

        val toUpdate = Update.newUpdate(eon, to.update.version + 1, to.address, toTxs)
        toUpdate.sign(to.kp)

        val updates = hub.transfer(tx, fromUpdate, toUpdate)
        from.update = updates[0]
        to.update = updates[1]
    }

    private fun withdraw(address: Address, amount: Long) {
//        this.txs.add(
//            ChainTransaction(
//                address,
//                Constants.CONTRACT_ADDRESS,
//                "InitiateWithdrawal",
//                //sirius.coreContractServiceGrpc.getInitiateWithdrawalMethod().getFullMethodName(),
//                InitiateWithdrawalRequest.newBuilder()
//                    .setAddress(address.toByteString())
//                    .setAmount(amount)
//                    //TODO
//                    //.setPath(hub.getProof(address)!!.toProto())
//                    .build()
//            )
//        )
        newBlock()
        Assert.assertEquals(amount, hub.getHubAccount(address)!!.withdraw)
        totalHubBalance.addAndGet(-amount)
    }

    private fun newBlock(): Block<*> {
        TODO()
        val blockInfo: Block<*> //(blockHeight.getAndIncrement())
//        if (this.txs.size > 0) {
//            this.txs.stream().peek { this.processTransaction(it) }
//                .forEach { blockInfo.addTransaction(it) }
//            this.txs = ArrayList()
//        }
        listenerReference!!.get()(blockInfo)
        return blockInfo
    }

    private fun waitHubReady() {
        val queue = hub.watchByFilter { event -> event.type === HubEventType.NEW_HUB_ROOT }
        // generate new block to return hub commit result, and trigger hub ready.
        newBlock()
        try {
            LOG.info("waitHubReady")
            queue.take()
        } catch (e: InterruptedException) {
            LOG.severe(e.message)
        }

    }

    private fun goToNextEon() {
        val expectEon = this.hub.currentEon().id + 1
        val queue = hub.watchByFilter { event -> event.type === HubEventType.NEW_HUB_ROOT }
        var event: HubEvent? = queue.poll()
        while (event == null || event.getPayload<HubRoot>().eon < expectEon) {
            this.newBlock()
            event = queue.poll()
        }
        Assert.assertEquals(expectEon, event.getPayload<HubRoot>().eon)
        this.verifyRoot()
    }

    private fun verifyRoot() {
        val newRoot = hub.stateRoot
        //TODO
        //Assert.assertEquals(this.globalBalance.getRoot().hubRoot2AugmentedMerkleTreeNode(), newRoot)
        Assert.assertEquals(totalHubBalance.get(), newRoot.allotment)
        this.root = newRoot
        if (this.a0.isRegister) {
            this.verifyProof(a0)
        }
        if (this.a1.isRegister) {
            this.verifyProof(a1)
        }
    }

    private fun verifyProof(account: LocalAccount) {
        val proof = hub.getProof(account.address)!!
        val proof1 = hub.getProof(hub.currentEon().id, account.address)
        Assert.assertEquals(proof, proof1)

        Assert.assertNotNull(proof)
        Assert.assertTrue(AMTree.verifyMembershipProof(root!!, proof))
        account.hubAccount = hub.getHubAccount(account.address)
    }
}
