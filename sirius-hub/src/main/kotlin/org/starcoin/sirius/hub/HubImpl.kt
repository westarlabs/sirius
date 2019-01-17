package org.starcoin.sirius.hub

import com.google.common.base.Preconditions
import com.google.common.eventbus.EventBus
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.*
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class HubImpl<T : ChainTransaction, A : ChainAccount>(
    private val owner: A,
    private val blocksPerEon: Int,
    private val chain: Chain<T, out Block<T>, out A>
) : Hub {

    companion object : WithLogging()

    private lateinit var contract: HubContract<A>  // = chain.getContract(QueryContractParameter(0))


    private lateinit var eonState: EonState


    private val hubAddress: Address = owner.address

    private val eventBus: EventBus = EventBus()

    private var ready: Boolean = false

    private val txReceipts = ConcurrentHashMap<Hash, CompletableFuture<Receipt>>()

    private val strategy: MaliciousStrategy = MaliciousStrategy()

    private lateinit var txChannel: Channel<TransactionResult<T>>
    private lateinit var blockChannel: Channel<out Block<T>>


    override val hubInfo: HubInfo
        get() {
            if (!this.ready) {
                return HubInfo(this.ready, this.blocksPerEon)
            }
            return HubInfo(
                ready,
                blocksPerEon,
                eonState.eon,
                stateRoot.toAMTreePathNode() as AMTreePathInternalNode,
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
        val currentHeight = chain.getBlockNumber()
        val contractHubInfo = contract.queryHubInfo(owner)
        val currentEon = Eon.calculateEon(currentHeight, contractHubInfo.blocksPerEon)
        LOG.info("ContractHubInfo: $contractHubInfo")
        //TODO load previous status from storage.
        eonState = EonState(currentEon.id)
        //first commit create by contract construct.
        // if miss latest commit, should commit root first.
        if (contractHubInfo.latestEon < currentEon.id) {
            this.doCommit()
        }
        this.txChannel = this.chain.watchTransactions { it.tx.to == hubAddress || it.tx.from == hubAddress }
        this.blockChannel = this.chain.watchBlock()
        this.processTransactions()
        this.processBlocks()
    }

    private fun processTransactions() {
        GlobalScope.launch {
            while (true) {
                val txResult = txChannel.receive()
                processTransaction(txResult)
            }
        }
    }

    private fun processBlocks() {
        GlobalScope.launch {
            while (true) {
                val block = blockChannel.receive()
                onBlock(block)
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

    override fun transfer(transaction: OffchainTransaction, fromUpdate: Update, toUpdate: Update): Array<Update> {
        this.checkReady()
        Preconditions.checkArgument(transaction.amount > BigInteger.ZERO, "transaction amount should > 0")
        val from = this.getHubAccount(transaction.from) ?: assertAccountNotNull(transaction.from)
        this.checkBalance(from, transaction.amount)
        this.processOffchainTransaction(transaction, fromUpdate, toUpdate)
        return arrayOf(fromUpdate, toUpdate)
    }

    private fun processOffchainTransaction(
        transaction: OffchainTransaction, fromUpdate: Update, toUpdate: Update
    ) {
        LOG.info(
            "processOffchainTransaction from:" + transaction.from + ", to:" + transaction.to
        )
        val from = this.getHubAccount(transaction.from) ?: assertAccountNotNull(transaction.from)
        val to = this.getHubAccount(transaction.to) ?: assertAccountNotNull(transaction.from)
        strategy.processOffchainTransaction(
            {
                from.appendTransaction(transaction, fromUpdate)
                to.appendTransaction(transaction, toUpdate)
            },
            transaction
        )
        fromUpdate.signHub(this.owner.key)
        toUpdate.signHub(this.owner.key)
        this.fireEvent(HubEvent(HubEventType.NEW_UPDATE, fromUpdate, from.address))
        this.fireEvent(HubEvent(HubEventType.NEW_UPDATE, toUpdate, to.address))
    }

    private fun fireEvent(event: HubEvent) {
        try {
            LOG.info("fireEvent:$event")
            this.eventBus.post(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        if (this.eonState.getIOUByFrom(iou.transaction.to) != null) {
            throw StatusRuntimeException(Status.ALREADY_EXISTS)
        }
        if (this.eonState.getIOUByTo(iou.transaction.to) != null) {
            throw StatusRuntimeException(Status.ALREADY_EXISTS)
        }
        this.checkIOU(iou, true)
        this.strategy.processSendNewTransaction(
            {
                this.eonState.addIOU(iou)
                this.fireEvent(
                    HubEvent(
                        HubEventType.NEW_TX, iou.transaction, iou.transaction.to
                    )
                )
            },
            iou
        )
    }

    override fun receiveNewTransfer(receiverIOU: IOU) {
        if (this.eonState.getIOUByTo(receiverIOU.transaction.to) == null) {
            throw StatusRuntimeException(Status.NOT_FOUND)
        }
        val iou = this.eonState.getIOUByFrom(receiverIOU.transaction.from) ?: throw StatusRuntimeException(
            Status.NOT_FOUND
        )
        this.checkIOU(receiverIOU, false)
        this.processOffchainTransaction(
            receiverIOU.transaction, iou.update, receiverIOU.update
        )
        this.eonState.removeIOU(receiverIOU)
    }

    override fun queryNewTransfer(address: Address): OffchainTransaction? {
        val iou = this.eonState.getIOUByTo(address)
        return if (iou == null) null else iou.transaction
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

    override fun watch(listener: Hub.HubEventListener) {
        this.eventBus.register(listener)
    }

    override fun watch(address: Address): BlockingQueue<HubEvent> {
        val blockingQueue = ArrayBlockingQueue<HubEvent>(5, false)
        this.watch(Hub.HubEventListener { event ->
            if (event.isPublicEvent || event.address == address) {
                blockingQueue.offer(event)
            }
        })
        return blockingQueue
    }

    override fun watchByFilter(predicate: (HubEvent) -> Boolean): BlockingQueue<HubEvent> {
        val blockingQueue = ArrayBlockingQueue<HubEvent>(5, false)
        this.watch(Hub.HubEventListener { event ->
            if (predicate(event)) {
                blockingQueue.offer(event)
            }
        })
        return blockingQueue
    }

    private fun doCommit(): CompletableFuture<Receipt> {
        val hubRoot =
            HubRoot(this.eonState.state.root.toAMTreePathNode() as AMTreePathInternalNode, this.eonState.eon)
        LOG.info("doCommit:" + hubRoot.toJSON())
        val txHash = this.contract.commit(owner, hubRoot)
        val future = CompletableFuture<Receipt>()
        future.whenComplete { receipt, throwable ->
            if (!receipt.status) {
                // TODO
                LOG.severe("commit tx receipt is failure.")
            } else {
                // TODO only
                this.ready = true
                this.fireEvent(
                    HubEvent(
                        HubEventType.NEW_HUB_ROOT,
                        HubRoot(
                            this.eonState.state.root.toAMTreePathNode() as AMTreePathInternalNode,
                            this.eonState.eon
                        )
                    )
                )
            }
        }
        this.txReceipts[txHash] = future
        return future
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
                CloseTransferDeliveryChallenge(accountProof, currentAccount.update, txProof, currentAccount.publicKey)
            this.contract.closeTransferDeliveryChallenge(owner, closeChallenge)
        } else {
            LOG.warning("Can not find tx Proof with challenge:" + challenge.toString())
        }
    }

    private fun processBalanceUpdateChallenge(challenge: BalanceUpdateChallenge) {
        val address = CryptoService.generateAddress(challenge.publicKey)
        //TODO is nullable
        val proofPath = this.eonState.state.getMembershipProof(address) ?: assertAccountNotNull(address)

        //val proof = BalanceUpdateProof(proofPath?.leaf?.nodeInfo?.update, proofPath)
        val closeBalanceUpdateChallengeRequest = CloseBalanceUpdateChallenge(proofPath.leaf.nodeInfo.update, proofPath)
        this.contract.closeBalanceUpdateChallenge(owner, closeBalanceUpdateChallengeRequest)
    }

    private fun processWithdrawal(withdrawal: Withdrawal) {
        this.strategy.processWithdrawal(
            {
                val address = withdrawal.address
                val amount = withdrawal.amount
                val hubAccount = this.eonState.getAccount(address) ?: assertAccountNotNull(address)
                if (!hubAccount.addWithdraw(amount)) {
                    //signed update (e) or update (e − 1), τ (e − 1)
                    //TODO path is nullable?
                    val path = this.eonState.state.getMembershipProof(address)
                    val cancelWithdrawal = CancelWithdrawal(address, hubAccount.update, path ?: AMTreeProof.DUMMY_PROOF)
                    val txHash = contract.cancelWithdrawal(owner, cancelWithdrawal)
                    val future = CompletableFuture<Receipt>()
                    this.txReceipts[txHash] = future
                    future.whenComplete { _, _ ->
                        // TODO ensure cancel success.
                        LOG.info(
                            ("CancelWithdrawal:"
                                    + address
                                    + ", amount:"
                                    + amount
                                    + ", result:"
                                    + future.getNow(null))
                        )

                        val withdrawalStatus = WithdrawalStatus(WithdrawalStatusType.INIT, withdrawal)
                        withdrawalStatus.cancel()
                        this.fireEvent(
                            HubEvent(HubEventType.WITHDRAWAL, withdrawalStatus, address)
                        )
                    }
                } else {
                    val withdrawalStatus = WithdrawalStatus(WithdrawalStatusType.INIT, withdrawal)
                    withdrawalStatus.pass()
                    this.fireEvent(HubEvent(HubEventType.WITHDRAWAL, withdrawalStatus, address))
                }
            },
            withdrawal
        )
    }

    private fun processDeposit(deposit: Deposit) {
        this.strategy.processDeposit(
            {
                val hubAccount = this.eonState.getAccount(deposit.address) ?: assertAccountNotNull(deposit.address)
                hubAccount.addDeposit(deposit.amount)
                this.fireEvent(
                    HubEvent(
                        HubEventType.NEW_DEPOSIT,
                        Deposit(deposit.address, deposit.amount),
                        deposit.address
                    )
                )
            },
            deposit
        )
    }

    override fun onBlock(blockInfo: Block<*>) {
        LOG.info("onBlock:$blockInfo")
        val eon = Eon.calculateEon(blockInfo.height, this.blocksPerEon)
        var newEon = false
        this.eonState.setEpoch(eon.epoch)
        if (eon.id != this.eonState.eon) {
            val eonState = EonState(eon.id, this.eonState)
            this.eonState = eonState
            newEon = true
        }
        if (newEon) {
            this.doCommit()
        }
    }

    private fun processTransaction(txResult: TransactionResult<T>) {
        val tx = txResult.tx
        val hash = tx.hash()
        txReceipts[hash]?.complete(txResult.receipt)
        val contractFunction = tx.contractFunction
        when (contractFunction) {
            null -> {
                val deposit = Deposit(tx.from!!, tx.amount)
                LOG.info("Deposit:" + deposit.toJSON())
                this.processDeposit(deposit)
            }
            is CommitFunction -> {
                contractFunction.decode(tx.data)
            }
            is InitiateWithdrawalFunction -> {
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processWithdrawal(input)
            }
            is OpenTransferDeliveryChallengeFunction -> {
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processTransferDeliveryChallenge(input)
            }
            is OpenBalanceUpdateChallengeFunction -> {
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                this.processBalanceUpdateChallenge(input)
            }
        }
    }

    override fun resetHubMaliciousFlag(): EnumSet<Hub.HubMaliciousFlag> {
        val result = this.hubMaliciousFlag
        this.maliciousFlags = EnumSet.noneOf(Hub.HubMaliciousFlag::class.java)
        return result
    }


    private inner class MaliciousStrategy {

        fun processDeposit(normalAction: () -> Unit, deposit: Deposit) {
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
            } else {
                normalAction()
            }
        }

        fun processWithdrawal(normalAction: () -> Unit, withdrawal: Withdrawal) {
            // steal withdrawal from a random user who has enough balance.
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_WITHDRAWAL)) {
                val hubAccount =
                    getHubAccount { account -> (account.address != withdrawal.address && account.balance >= withdrawal.amount) }
                if (hubAccount != null) {
                    hubAccount.addWithdraw(withdrawal.amount)
                    LOG.info(
                        (withdrawal.address.toString()
                                + " steal withdrawal from "
                                + hubAccount.address.toString())
                    )
                } else {
                    normalAction()
                }
            } else {
                normalAction()
            }
        }

        fun processOffchainTransaction(normalAction: () -> Unit, tx: OffchainTransaction) {
            // steal transaction, not real update account's tx.
            if (maliciousFlags.contains(Hub.HubMaliciousFlag.STEAL_TRANSACTION)) {
                LOG.info("steal transaction:" + tx.toJSON())
                // do nothing
            } else {
                normalAction()
            }
        }

        fun processSendNewTransaction(normalAction: () -> Unit, sendIOU: IOU) {
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

                from.appendTransaction(sendIOU.transaction, fromUpdate)
                to.appendTransaction(tx, toUpdate)

                fromUpdate.signHub(owner.key)
                toUpdate.signHub(owner.key)
                // only notice from.
                fireEvent(HubEvent(HubEventType.NEW_UPDATE, fromUpdate, from.address))
            } else {
                normalAction()
            }
        }
    }
}
