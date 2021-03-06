package org.starcoin.sirius.core

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.Alias
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import java.security.PublicKey
import java.util.*

@Serializable
@ProtobufSchema(Starcoin.AccountEonState::class)
data class AccountEonState(
    @SerialId(1)
    var update: Update,
    @SerialId(2)
    @Serializable(with = BigIntegerSerializer::class)
    var allotment: BigInteger = BigInteger.ZERO,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    var deposit: BigInteger = BigInteger.ZERO,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    var withdraw: BigInteger = BigInteger.ZERO,
    @SerialId(5)
    @Optional
    internal val txs: MutableList<OffchainTransaction> = mutableListOf(),
    @SerialId(6)
    @Optional
    internal val pendingSendTxs: MutableList<IOU> = mutableListOf(),
    @SerialId(7)
    @Optional
    internal val pendingReceiveTxs: MutableList<OffchainTransaction> = mutableListOf()
) : SiriusObject() {

    constructor(eon: Int) : this(Update(UpdateData(eon)))

    init {
        assert(checkBalance())
    }

    @Transient
    val balance: BigInteger
        get() = ((this.allotment
                + deposit
                + update.data.receiveAmount)
                - this.withdraw
                - update.data.sendAmount)


    fun isEmpty(): Boolean {
        return this.txs.isEmpty() && this.pendingReceiveTxs.isEmpty() && this.pendingSendTxs.isEmpty() && update.isEmpty() && allotment == BigInteger.ZERO && deposit == BigInteger.ZERO && withdraw == BigInteger.ZERO
    }

    fun isNotEmpty() = !isEmpty()

    internal fun checkBalance(amount: BigInteger = BigInteger.ZERO): Boolean {
        return this.balance >= amount
    }

    companion object : SiriusObjectCompanion<AccountEonState, Starcoin.AccountEonState>(AccountEonState::class) {
        override fun mock(): AccountEonState {
            val update = Update.mock()
            val deposit = MockUtils.nextBigInteger()
            val withdraw = MockUtils.nextBigInteger()
            val allotment =
                (update.receiveAmount + deposit - withdraw - update.sendAmount).abs() + MockUtils.nextBigInteger()

            val state = AccountEonState(
                update,
                allotment,
                deposit,
                withdraw
            )
            if (MockUtils.nextBoolean()) {
                for (i in 1..MockUtils.nextInt(2, 10)) {
                    state.txs.add(OffchainTransaction.mock())
                }
            }
            if (MockUtils.nextBoolean()) {
                for (i in 1..MockUtils.nextInt(2, 10)) {
                    state.pendingReceiveTxs.add(OffchainTransaction.mock())
                }
            }
            if (MockUtils.nextBoolean()) {
                for (i in 1..MockUtils.nextInt(2, 10)) {
                    state.pendingSendTxs.add(IOU.mock())
                }
            }
            return state
        }
    }
}

@Serializable
@ProtobufSchema(Starcoin.HubAccount::class)
data class HubAccount(
    @Serializable(with = PublicKeySerializer::class)
    @SerialId(1)
    val publicKey: PublicKey,
    @SerialId(2)
    val eonState: AccountEonState
) : SiriusObject() {

    constructor(
        publicKey: PublicKey,
        update: Update,
        allotment: BigInteger = BigInteger.ZERO,
        deposit: BigInteger = BigInteger.ZERO,
        withdraw: BigInteger = BigInteger.ZERO,
        transactions: MutableList<OffchainTransaction> = mutableListOf()
    ) : this(publicKey, AccountEonState(update, allotment, deposit, withdraw, transactions))

    constructor(
        publicKey: PublicKey,
        update: Update,
        allotment: Long = 0,
        deposit: Long = 0,
        withdraw: Long = 0,
        transactions: MutableList<OffchainTransaction> = mutableListOf()
    ) : this(
        publicKey,
        AccountEonState(update, allotment.toBigInteger(), deposit.toBigInteger(), withdraw.toBigInteger(), transactions)
    )

    @Transient
    val address = Address.getAddress(publicKey)

    @Transient
    var update: Update by Alias(eonState::update)

    @Transient
    var allotment: BigInteger by Alias(eonState::allotment)

    @Transient
    var deposit: BigInteger by Alias(eonState::deposit)

    @Transient
    var withdraw: BigInteger by Alias(eonState::withdraw)

    @Transient
    val balance: BigInteger
        get() = eonState.balance

    // pending Send tx only support one currently.
    fun getPendingSendTx(): IOU? {
        return this.eonState.pendingSendTxs.firstOrNull { it.transaction.from == this.address }
    }

    fun getPendingReceiveTxs(): List<OffchainTransaction> {
        return this.eonState.pendingReceiveTxs
    }

    private fun removePendingSendTx(txHash: Hash): Boolean {
        return this.eonState.pendingSendTxs.removeIf { it.transaction.hash() == txHash }
    }

    private fun removePendingReceiveTx(txHash: Hash): Boolean {
        return this.eonState.pendingReceiveTxs.removeIf { it.hash() == txHash }
    }

    fun confirmTransaction(tx: OffchainTransaction, update: Update, skipCheck: Boolean = false) {
        var isSender = false
        if (!skipCheck) {
            when {
                tx.from == this.address -> {
                    isSender = true;check(this.getPendingSendTx()?.transaction == tx)
                    "Can not find pending tx:${tx.hash()}"
                }
                tx.to == this.address -> check(this.getPendingReceiveTxs().find { it.hash() == tx.hash() } != null) {
                    "Can not find tx:${tx.hash()}"
                }
                else -> throw IllegalArgumentException("Unexpected tx: ${tx.hash()}")
            }
            this.checkUpdate(tx, update)
        }
        this.eonState.txs.add(tx)
        //TODO set update to val.
        this.update = update
        if (isSender) {
            val remove = this.removePendingSendTx(tx.hash())
            if (!skipCheck) {
                check(remove)
            }
        } else {
            val remove = this.removePendingReceiveTx(tx.hash())
            if (!skipCheck) {
                check(remove)
            }
        }
    }

    fun getTransactions(): List<OffchainTransaction> {
        return this.eonState.txs
    }

    fun appendSendTx(iou: IOU) {
        this.checkIOU(iou)
        this.eonState.pendingSendTxs.add(iou)
    }

    fun appendReceiveTx(tx: OffchainTransaction) {
        this.eonState.pendingReceiveTxs.add(tx)
    }

    fun addDeposit(amount: Long) = this.addDeposit(amount.toBigInteger())

    fun addDeposit(amount: BigInteger) {
        this.deposit = this.deposit + amount
    }

    fun addWithdraw(amount: Long) = this.addWithdraw(amount.toBigInteger())

    fun addWithdraw(amount: BigInteger): Boolean {
        if (this.balance - amount >= BigInteger.ZERO) {
            this.withdraw = this.withdraw + amount
            return true
        }
        return false
    }

    private fun checkUpdate(newTx: OffchainTransaction, newUpdate: Update) {
        val sendTxs = ArrayList(this.eonState.txs)
        sendTxs.add(newTx)
        val prepareUpdate = UpdateData.newUpdate(newUpdate.data.eon, newUpdate.data.version, this.address, sendTxs)

        require(newUpdate.data.root == prepareUpdate.root) {
            "check ${this.address} update root hash fail, expect: ${prepareUpdate.root}, but get ${newUpdate.data.root}"
        }

        require(newUpdate.data.sendAmount == prepareUpdate.sendAmount) {
            "expect sendAmount ${newUpdate.data.sendAmount}, but get ${prepareUpdate.sendAmount}"
        }
        require(newUpdate.data.receiveAmount == prepareUpdate.receiveAmount) {
            "expect receiveAmount ${prepareUpdate.receiveAmount}, but get ${newUpdate.data.receiveAmount}"
        }
        require(newUpdate.data.version > update.data.version)
        require(checkBalance()) { "has not enough balance." }
    }

    private fun checkBalance(amount: BigInteger = BigInteger.ZERO): Boolean {
        return this.eonState.checkBalance(amount)
    }

    fun checkIOU(iou: IOU) {
        val transaction = iou.transaction
        require(transaction.amount > BigInteger.ZERO) { "transaction amount should > 0" }
        require(transaction.from != transaction.to) { "can not transfer to self." }
        val isSender = iou.transaction.from == this.address
        if (isSender) {
            val preIOU = this.getPendingSendTx()
            check(preIOU == null) { "exist a pending transaction." }
            this.checkBalance(transaction.amount)
            require(transaction.verify(this.publicKey)) { "transaction verify fail." }
            check(this.balance >= transaction.amount) { "account balance is not enough." }
        }
        checkUpdate(iou)
    }

    private fun checkUpdate(iou: IOU) {
        check(iou.update.verifySig(this.publicKey)) { "Update signature miss match." }
        LOG.fine(
            "iou version ${iou.update.version},server version ${this.update.version} "
        )
        check(iou.update.version > this.update.version) { "Update version should > previous version" }

        val txs = mutableListOf<OffchainTransaction>().apply { addAll(eonState.txs) }
        txs.add(iou.transaction)
        val merkleTree = MerkleTree(txs)
        check(
            iou.update.root == merkleTree.hash()
        ) { "Merkle root miss match." }
    }

    fun calculateNewAllotment(): BigInteger {
        val allotment = this.balance
        assert(allotment >= BigInteger.ZERO)
        return allotment
    }

    fun toNextEon(eon: Int): HubAccount {
        //TODO process pending tx
        val allotment = this.calculateNewAllotment()
        return HubAccount(publicKey, AccountEonState(Update(UpdateData(eon)), allotment))
    }

    companion object : SiriusObjectCompanion<HubAccount, Starcoin.HubAccount>(HubAccount::class) {
        override fun mock(): HubAccount {
            return HubAccount(
                CryptoService.generateCryptoKey().keyPair.public,
                AccountEonState.mock()
            )
        }
    }
}
