package org.starcoin.sirius.hub

import com.google.common.base.Preconditions
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import org.starcoin.sirius.channel.EventBus
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.*
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates

sealed class HubAction {
    data class IOUAction(val iou: IOU, val response: Channel<Exception?>) : HubAction()
    data class OffchainTransactionAction(
        val tx: OffchainTransaction,
        val fromUpdate: Update,
        val toUpdate: Update,
        val extAction: () -> Unit = {}
    ) : HubAction()

    data class BlockAction<T : ChainTransaction>(val block: Block<T>) : HubAction()
    //data class ChainTransactionAction<T : ChainTransaction>(val txResult: TransactionResult<T>) : HubAction()
}

enum class HubStatus {
    Prepare,
    Ready,
    Recovery
}

class HubImpl<A : ChainAccount>(
    private val owner: A,
    private val chain: Chain<out ChainTransaction, out Block<out ChainTransaction>, A>,
    private val contract: HubContract<A>
) : Hub {

    companion object : WithLogging()

    private lateinit var eonState: EonState


    private val ownerAddress: Address = owner.address

    private val eventBus = EventBus<HubEvent>()

    val ready: Boolean
        get() = hubStatus == HubStatus.Ready

    val recoveryMode: Boolean
        get() = hubStatus == HubStatus.Recovery

    private val txReceipts = ConcurrentHashMap<Hash, CompletableFuture<Receipt>>()

    private val strategy: MaliciousStrategy = MaliciousStrategy()

    var blocksPerEon = 0
        private set

    var startBlockNumber: Long by Delegates.notNull()
        private set
    var currentBlockNumber: Long by Delegates.notNull()

    private val withdrawals = ConcurrentHashMap<Address, Withdrawal>()

    private var hubStatus = HubStatus.Prepare

    private var processBlockJob: Job by Delegates.notNull()

    val hubActor = GlobalScope.actor<HubAction>(capacity = 100, onCompletion = {
        LOG.info("HubActor completion, exception:$it")
    }) {
        consumeEach {
            when (it) {
                is HubAction.IOUAction -> {
                    try {
                        strategy.processSendNewTransaction(it.iou)
                        {
                            eonState.addIOU(it.iou)
                            fireEvent(
                                HubEvent(
                                    HubEventType.NEW_TX, it.iou.transaction, it.iou.transaction.to
                                )
                            )
                        }
                        it.response.send(null)
                    } catch (e: Exception) {
                        it.response.send(e)
                    }
                }
                is HubAction.OffchainTransactionAction -> {
                    processOffchainTransaction(
                        it.tx, it.fromUpdate, it.toUpdate
                    )
                    it.extAction()
                }
                is HubAction.BlockAction<*> -> {
                    processBlock(it.block)
                }
            }
        }
    }

    override val hubInfo: HubInfo
        get() {
            if (!this.ready) {
                return HubInfo(this.ready, this.recoveryMode, this.blocksPerEon)
            }
            return HubInfo(
                ready,
                this.recoveryMode,
                blocksPerEon,
                eonState.eon,
                stateRoot.toAMTreePathNode() as AMTreePathNode,
                owner.key.keyPair.public
            )
        }

    override val stateRoot: AMTreeNode
        get() = this.eonState.state.root

    override var hubMaliciousFlag: EnumSet<Hub.HubMaliciousFlag>
        get() = EnumSet.copyOf(this.maliciousFlags)
        set(flags) {
            this.maliciousFlags.addAll(flags)
        }

    private var maliciousFlags: EnumSet<Hub.HubMaliciousFlag> = EnumSet.noneOf(Hub.HubMaliciousFlag::class.java)
    private val gang: ParticipantGang by lazy {
        val gang = ParticipantGang.random()
        val update = Update(UpdateData(currentEon().id))
        update.sign(gang.privateKey)
        registerParticipant(gang.participant, update)
        gang
    }

    override fun start() {
        this.currentBlockNumber = chain.getBlockNumber()
        LOG.info("CurrentBlockNumber: $currentBlockNumber")
        //contract.setHubIp(owner, "127.0.0.1:8484")
        val contractHubInfo = contract.queryHubInfo(owner)
        LOG.info("ContractHubInfo: $contractHubInfo")

        this.blocksPerEon = contractHubInfo.blocksPerEon
        this.startBlockNumber = contractHubInfo.startBlockNumber.longValueExact()
        val currentEon = Eon.calculateEon(startBlockNumber, currentBlockNumber, blocksPerEon)

        //TODO load previous status from storage.
        eonState = EonState(currentEon.id)

        this.processBlockJob = GlobalScope.launch(start = CoroutineStart.LAZY) {
            val blockChannel = chain.watchBlock()
            for (block in blockChannel) {
                hubActor.send(HubAction.BlockAction(block))
            }
        }
        val recoveryMode = contract.isRecoveryMode(owner)
        if (recoveryMode) {
            this.hubStatus = HubStatus.Recovery
        } else {
            this.processBlockJob.start()
            //first commit create by contract construct.
            //if miss latest commit, should commit root first.
            if (contractHubInfo.latestEon < currentEon.id) {
                this.doCommit()
            } else {
                this.hubStatus = HubStatus.Ready
            }
        }
    }

    fun getEonState(eon: Int): EonState? {
        var eonState = this.eonState
        if (eonState.eon < eon) {
            return null
        }
        if (eonState.eon == eon) {
            return eonState
        }
        while (eon <= eonState.eon) {
            if (eonState.eon == eon) {
                return eonState
            }
            eonState = eonState.previous ?: return null
        }
        return null
    }

    override fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        this.checkReady()
        Preconditions.checkArgument(initUpdate.verifySig(participant.publicKey))
        if (this.getHubAccount(participant.address) != null) {
            throw StatusRuntimeException(Status.ALREADY_EXISTS)
        }
        initUpdate.signHub(this.owner.key)
        val account = HubAccount(participant.publicKey, initUpdate, 0)
        this.eonState.addAccount(account)
        return initUpdate
    }

    override fun deposit(participant: Address, amount: Long) {
        val account = this.eonState.getAccount(participant) ?: assertAccountNotNull(participant)
        account.addDeposit(amount)
        this.eonState.saveAccount(account)
    }

    override fun getHubAccount(address: Address): HubAccount? {
        return this.getHubAccount(this.eonState.eon, address)
    }

    override fun getHubAccount(eon: Int, address: Address): HubAccount? {
        this.checkReady()
        return this.getEonState(eon)?.getAccount(address)
    }

    fun getHubAccount(predicate: (HubAccount) -> Boolean): HubAccount? {
        this.checkReady()
        return this.eonState.getAccount(predicate)
    }

    private suspend fun processOffchainTransaction(
        transaction: OffchainTransaction, fromUpdate: Update, toUpdate: Update
    ) {
        LOG.info(
            "processOffchainTransaction from:" + transaction.from + ", to:" + transaction.to
        )
        val from = this.getHubAccount(transaction.from) ?: assertAccountNotNull(transaction.from)
        val to = this.getHubAccount(transaction.to) ?: assertAccountNotNull(transaction.from)
        strategy.processOffchainTransaction(transaction)
        {
            from.confirmTransaction(transaction, fromUpdate)
            to.confirmTransaction(transaction, toUpdate)
        }

        fromUpdate.signHub(this.owner.key)
        toUpdate.signHub(this.owner.key)

        eonState.saveAccount(from)
        eonState.saveAccount(to)

        this.fireEvent(HubEvent(HubEventType.NEW_UPDATE, fromUpdate, from.address))
        this.fireEvent(HubEvent(HubEventType.NEW_UPDATE, toUpdate, to.address))
    }

    private suspend fun fireEvent(event: HubEvent) {
        LOG.info("fireEvent:$event")
        this.eventBus.send(event)
    }

    private fun checkBalance(account: HubAccount, amount: BigInteger) {
        Preconditions.checkState(account.balance >= amount)
    }

    fun checkIOU(iou: IOU, isSender: Boolean) {
        this.checkReady()
        val transaction = iou.transaction
        Preconditions.checkArgument(transaction.amount > BigInteger.ZERO, "transaction amount should > 0")
        val sender = this.getHubAccount(transaction.from) ?: assertAccountNotNull(transaction.from)
        val to = this.getHubAccount(transaction.to) ?: assertAccountNotNull(transaction.from)
        Preconditions.checkArgument(
            transaction.verify(sender.publicKey), "transaction verify fail."
        )
        this.checkBalance(sender, transaction.amount)
        if (isSender) {
            checkUpdate(sender, iou)
        } else {
            checkUpdate(to, iou)
        }
    }

    override fun sendNewTransfer(iou: IOU) {
        //if (this.eonState.getIOUByFrom(iou.transaction.from) != null) throw StatusRuntimeException(Status.ALREADY_EXISTS)
        val deferred = GlobalScope.async {
            val response = Channel<Exception?>()
            hubActor.send(HubAction.IOUAction(iou, response))
            response.receiveOrNull()
        }
        //TODO async
        val exception = runBlocking {
            deferred.await()
        }
        if (exception != null) {
            throw exception
        }
    }

    override fun receiveNewTransfer(receiverIOU: IOU) {
        val iou = this.eonState.getPendingSendTx(receiverIOU.transaction.from) ?: throw StatusRuntimeException(
            Status.NOT_FOUND
        )
        this.checkIOU(receiverIOU, false)
        GlobalScope.launch {
            hubActor.send(
                HubAction.OffchainTransactionAction(
                    receiverIOU.transaction,
                    iou.update,
                    receiverIOU.update
                )
            )
        }

    }

    override fun queryNewTransfer(address: Address): List<OffchainTransaction> {
        return eonState.getPendingReceiveTxs(address)
    }

    private fun checkUpdate(account: HubAccount, iou: IOU) {
        Preconditions.checkState(
            iou.update.verifySig(account.publicKey), "Update signature miss match."
        )
        LOG.info(
            String.format(
                "iou version %d,server version %d ",
                iou.update.version, account.update.version
            )
        )
        Preconditions.checkState(
            iou.update.version > account.update.version,
            "Update version should > previous version"
        )
        val txs = ArrayList(account.getTransactions())
        txs.add(iou.transaction)
        val merkleTree = MerkleTree(txs)
        Preconditions.checkState(
            iou.update.root.equals(merkleTree.hash()), "Merkle root miss match."
        )
    }

    override fun getProof(address: Address): AMTreeProof? {
        return this.getProof(this.eonState.eon, address)
    }

    override fun getProof(eon: Int, address: Address): AMTreeProof? {
        this.checkReady()
        val eonState = this.getEonState(eon) ?: return null
        return eonState.state.getMembershipProof(address)
    }

    override fun currentEon(): Eon {
        return Eon(eonState.eon, eonState.currentEpoch)
    }

    private fun checkReady() {
        Preconditions.checkState(this.ready, "Hub is not ready for service, please wait.")
    }

    override fun watch(address: Address): ReceiveChannel<HubEvent> {
        return this.watch { it.isPublicEvent || it.address == address }
    }

    override fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent> {
        return this.eventBus.subscribe(predicate)
    }

    private fun doCommit() {
        val hubRoot =
            HubRoot(this.eonState.state.root.toAMTreePathNode(), this.eonState.eon)
        LOG.info("doCommit currentBlockNumber:$currentBlockNumber, root:$hubRoot")
        this.contract.commit(owner, hubRoot)
    }

    fun assertAccountNotNull(to: Address): Nothing =
        throw RuntimeException("Can not find account by address: $to")

    private fun processTransferDeliveryChallenge(challenge: TransferDeliveryChallenge) {
        val tx = challenge.tx

        val to = tx.to
        val currentAccount = this.eonState.getAccount(to) ?: assertAccountNotNull(to)
        val accountProof = this.eonState.state.getMembershipProof(to) ?: assertAccountNotNull(to)
        val previousAccount = this.eonState.previous?.getAccount(to)

        var txProof: MerklePath? = null
        val txs = previousAccount?.getTransactions() ?: emptyList()
        if (!txs.isEmpty()) {
            val merkleTree = MerkleTree(txs)
            txProof = merkleTree.getMembershipProof(tx.hash())
        }
        if (txProof != null) {
            val closeChallenge =
                CloseTransferDeliveryChallenge(accountProof, txProof, currentAccount.address, tx.hash())
            //assert(AMTree.verifyMembershipProof(this.contract.getLatestRoot(owner)!!.root, closeChallenge.proof))
            this.contract.closeTransferDeliveryChallenge(owner, closeChallenge)
        } else {
            LOG.warning("Can not find tx Proof with challenge:" + challenge.toString())
        }
    }

    private fun processBalanceUpdateChallenge(address: Address, challenge: BalanceUpdateProof) {
        //TODO is nullable
        val proofPath = this.eonState.state.getMembershipProof(address) ?: assertAccountNotNull(address)
        this.contract.closeBalanceUpdateChallenge(owner, CloseBalanceUpdateChallenge(address, proofPath))
    }

    private suspend fun processWithdrawal(from: Address, withdrawal: Withdrawal) {
        withdrawals[from] = withdrawal
        this.strategy.processWithdrawal(from, withdrawal)
        {
            val amount = withdrawal.amount
            val hubAccount = this.eonState.getAccount(from) ?: assertAccountNotNull(from)
            this.withdrawals[from] = withdrawal
            if (!hubAccount.addWithdraw(amount)) {
                //signed update (e) or update (e − 1), τ (e − 1)
                //TODO path is nullable?
                val path = this.eonState.state.getMembershipProof(from)
                val cancelWithdrawal = CancelWithdrawal(from, hubAccount.update, path ?: AMTreeProof.DUMMY_PROOF)
                contract.cancelWithdrawal(owner, cancelWithdrawal)
            } else {
                eonState.saveAccount(hubAccount)
                val withdrawalStatus = WithdrawalStatus(WithdrawalStatusType.INIT, withdrawal)
                withdrawalStatus.pass()
                this.fireEvent(HubEvent(HubEventType.WITHDRAWAL, withdrawalStatus, from))
            }
        }

    }

    private suspend fun processDeposit(deposit: Deposit) {
        this.strategy.processDeposit(deposit)
        {
            val hubAccount = this.eonState.getAccount(deposit.address) ?: assertAccountNotNull(deposit.address)
            hubAccount.addDeposit(deposit.amount)
            this.eonState.saveAccount(hubAccount)
            this.fireEvent(
                HubEvent(
                    HubEventType.NEW_DEPOSIT,
                    deposit,
                    deposit.address
                )
            )
        }
    }

    private suspend fun processBlock(block: Block<*>) {
        LOG.info("Hub processBlock:$block")
        this.currentBlockNumber = block.height
        val eon = Eon.calculateEon(this.startBlockNumber, block.height, this.blocksPerEon)
        var newEon = false
        this.eonState.setEpoch(eon.epoch)
        if (eon.id != this.eonState.eon) {
            val eonState = EonState(eon.id, this.eonState)
            this.eonState = eonState
            newEon = true
        }

        block.transactions.filter { it.tx.to == contract.contractAddress }.forEach {
            processTransaction(block, it)
        }

        if (newEon) {
            this.doCommit()
        }
    }

    private fun doRecovery() {
        LOG.warning("Hub entry RecoveryMode, stop process block.")
        this.hubStatus = HubStatus.Recovery
        this.processBlockJob.cancel()
    }

    private suspend fun processTransaction(block: Block<*>, txResult: TransactionResult<*>) {
        LOG.info("Hub process tx: ${txResult.tx.hash()}, ${block.height} result: ${txResult.receipt}")
        if (!txResult.receipt.status) {
            LOG.warning("tx ${txResult.tx.hash()} status is fail.")
        }
        if (txResult.receipt.recoveryMode) {
            LOG.info("tx:${txResult.tx.hash()} trigger recoveryMode.")
            this.doRecovery()
            return
        }
        val tx = txResult.tx
        val hash = tx.hash()
        txReceipts[hash]?.complete(txResult.receipt)
        val contractFunction = tx.contractFunction
        when (contractFunction) {
            null -> {
                if (!txResult.receipt.status) {
                    return
                }
                val deposit = Deposit(tx.from!!, tx.amount)
                LOG.info("Deposit:" + deposit.toJSON())
                val blockNumber = currentBlockNumber //txResult.receipt.blockNumber
                val eon = Eon.calculateEon(startBlockNumber, blockNumber, blocksPerEon)
                if (eon.id > this.currentEon().id) {
                    GlobalScope.launch {
                        while (eon.id > currentEon().id) {
                            //TODO
                            LOG.info("Receive next new eon Deposit, so wait.")
                            delay(1000)
                        }
                        processDeposit(deposit)
                    }
                } else {
                    this.processDeposit(deposit)
                }
            }
            is CommitFunction -> {
                val hubRoot = contractFunction.decode(tx.data)!!
                if (txResult.receipt.status) {
                    this.hubStatus = HubStatus.Ready
                    this.fireEvent(
                        HubEvent(
                            HubEventType.NEW_HUB_ROOT,
                            hubRoot
                        )
                    )
                } else {
                    //TODO retry commit ?
                    LOG.warning("Commit hub root $hubRoot fail, hub entry recoveryMode.")
                    doRecovery()
                }
            }
            is InitiateWithdrawalFunction -> {
                if (!txResult.receipt.status) {
                    return
                }
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processWithdrawal(tx.from!!, input)
            }
            is OpenTransferDeliveryChallengeFunction -> {
                if (!txResult.receipt.status) {
                    return
                }
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processTransferDeliveryChallenge(input)
            }
            is OpenBalanceUpdateChallengeFunction -> {
                if (!txResult.receipt.status) {
                    return
                }
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processBalanceUpdateChallenge(tx.from!!, input)
            }
            is CancelWithdrawalFunction -> {
                if (!txResult.receipt.status) {
                    return
                }
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                val withdrawal = this.withdrawals[input.address]!!
                LOG.info("CancelWithdrawal: ${input.address}, amount: ${withdrawal.amount}")
                val withdrawalStatus = WithdrawalStatus(WithdrawalStatusType.INIT, withdrawal)
                withdrawalStatus.cancel()
                this.fireEvent(
                    HubEvent(HubEventType.WITHDRAWAL, withdrawalStatus, input.address)
                )
            }
        }
    }

    override fun resetHubMaliciousFlag(): EnumSet<Hub.HubMaliciousFlag> {
        val result = this.hubMaliciousFlag
        this.maliciousFlags = EnumSet.noneOf(Hub.HubMaliciousFlag::class.java)
        return result
    }


    private inner class MaliciousStrategy {

        suspend fun processDeposit(deposit: Deposit, normalAction: suspend () -> Unit) {
            // steal deposit to a hub gang Participant
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_DEPOSIT)) {
                LOG.info(
                    gang.participant.address.toString()
                            + " steal deposit from "
                            + deposit.address.toString()
                )
                val hubAccount =
                    eonState.getAccount(gang.participant.address) ?: assertAccountNotNull(gang.participant.address)
                hubAccount.addDeposit(deposit.amount)
                eonState.saveAccount(hubAccount)
            } else {
                normalAction()
            }
        }

        suspend fun processWithdrawal(from: Address, withdrawal: Withdrawal, normalAction: suspend () -> Unit) {
            // steal withdrawal from a random user who has enough balance.
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL)) {
                val hubAccount =
                    getHubAccount { account -> (account.address != from && account.balance >= withdrawal.amount) }
                if (hubAccount != null) {
                    hubAccount.addWithdraw(withdrawal.amount)
                    LOG.info(
                        (from.toString()
                                + " steal withdrawal from "
                                + hubAccount.address.toString())
                    )
                    eonState.saveAccount(hubAccount)
                } else {
                    normalAction()
                }
            } else {
                normalAction()
            }
        }

        suspend fun processOffchainTransaction(tx: OffchainTransaction, normalAction: suspend () -> Unit) {
            // steal transaction, not real update account's tx.
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION)) {
                LOG.info("steal transaction:" + tx.toJSON())
                // do nothing
            } else {
                normalAction()
            }
        }

        suspend fun processSendNewTransaction(sendIOU: IOU, normalAction: suspend () -> Unit) {
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION_IOU)) {
                LOG.info("steal transaction iou from:" + sendIOU.transaction.from)
                checkIOU(sendIOU, true)
                val tx = OffchainTransaction(
                    sendIOU.transaction.eon,
                    sendIOU.transaction.from,
                    gang.participant.address,
                    sendIOU.transaction.amount
                )

                val from = getHubAccount(sendIOU.transaction.from) ?: assertAccountNotNull(sendIOU.transaction.from)

                val to = getHubAccount(gang.participant.address) ?: assertAccountNotNull(gang.participant.address)
                val sendTxs = ArrayList(to.getTransactions())
                sendTxs.add(tx)
                val toUpdate = Update.newUpdate(
                    to.update.eon, to.update.version + 1, to.address, sendTxs
                )
                toUpdate.sign(gang.privateKey)

                val fromUpdate = sendIOU.update

                from.confirmTransaction(sendIOU.transaction, fromUpdate)
                to.confirmTransaction(tx, toUpdate)

                fromUpdate.signHub(owner.key)
                toUpdate.signHub(owner.key)

                eonState.saveAccount(from)
                eonState.saveAccount(to)
                // only notice from.
                fireEvent(HubEvent(HubEventType.NEW_UPDATE, fromUpdate, from.address))
            } else {
                normalAction()
            }
        }
    }

    override fun stop() {
        this.processBlockJob.cancel()
    }
}
