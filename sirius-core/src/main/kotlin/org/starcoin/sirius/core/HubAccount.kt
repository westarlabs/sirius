package org.starcoin.sirius.core

import com.google.common.base.Preconditions
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import java.security.PublicKey
import java.util.*
import java.util.stream.Collectors

@Serializable
@ProtobufSchema(Starcoin.HubAccount::class)
data class HubAccount(
    @Serializable(with = PublicKeySerializer::class)
    @SerialId(1)
    val publicKey: PublicKey,
    @SerialId(2)
    var update: Update,
    @SerialId(3)
    @Serializable(with = BigIntegerSerializer::class)
    var allotment: BigInteger = BigInteger.ZERO,
    @SerialId(4)
    @Serializable(with = BigIntegerSerializer::class)
    var deposit: BigInteger = BigInteger.ZERO,
    @SerialId(5)
    @Serializable(with = BigIntegerSerializer::class)
    var withdraw: BigInteger = BigInteger.ZERO,
    @SerialId(6)
    @Optional
    private val transactions: MutableList<OffchainTransaction> = mutableListOf()
) : SiriusObject() {
    init {
        assert(checkBalance())
    }

    constructor(
        publicKey: PublicKey,
        update: Update,
        allotment: Long = 0,
        deposit: Long = 0,
        withdraw: Long = 0,
        transactions: MutableList<OffchainTransaction> = mutableListOf()
    ) : this(publicKey, update, allotment.toBigInteger(), deposit.toBigInteger(), withdraw.toBigInteger(), transactions)

    @Transient
    val address = Address.getAddress(publicKey)

    @Transient
    val balance: BigInteger
        get() = ((this.allotment
                + deposit
                + update.data.receiveAmount)
                - this.withdraw
                - update.data.sendAmount)

    fun appendTransaction(tx: OffchainTransaction, update: Update) {
        this.checkUpdate(tx, update)
        this.transactions.add(tx)
        //TODO set update to val.
        this.update = update
    }

    fun getTransactions(): List<OffchainTransaction> {
        return Collections.unmodifiableList(transactions)
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
        val sendTxs = ArrayList(this.transactions)
        sendTxs.add(newTx)
        val prepareUpdate = UpdateData.newUpdate(newUpdate.data.eon, newUpdate.data.version, this.address!!, sendTxs)

        Preconditions.checkArgument(
            newUpdate.data.root == prepareUpdate.root,
            "check " + this.address + " update root hash fail, expect:" + prepareUpdate.root!!
                .toMD5Hex() + ", but get " + newUpdate.data.root.toMD5Hex()
        )

        Preconditions.checkArgument(
            newUpdate.data.sendAmount == prepareUpdate.sendAmount, "sendAmount"
        )
        Preconditions.checkArgument(
            newUpdate.data.receiveAmount == prepareUpdate.receiveAmount,
            String.format(
                "expect receiveAmount %s, but get %s",
                prepareUpdate.receiveAmount, newUpdate.data.receiveAmount
            )
        )
        Preconditions.checkArgument(newUpdate.data.version > update.data.version)
        Preconditions.checkArgument(checkBalance(), "has not enough balance.")
    }

    private fun checkBalance(): Boolean {
        return this.balance >= BigInteger.ZERO
    }

    fun calculateNewAllotment(): BigInteger {
        val allotment = this.balance
        assert(allotment >= BigInteger.ZERO)
        return allotment
    }

    fun toNextEon(eon: Int): HubAccount {
        val allotment = this.calculateNewAllotment()
        return HubAccount(publicKey, Update(UpdateData(eon)), allotment)
    }

    companion object : SiriusObjectCompanion<HubAccount, Starcoin.HubAccount>(HubAccount::class) {
        override fun mock(): HubAccount {
            val update = Update.mock()
            val deposit = MockUtils.nextBigInteger()
            val withdraw = MockUtils.nextBigInteger()
            val allotment =
                (update.receiveAmount + deposit - withdraw - update.sendAmount).abs() + MockUtils.nextBigInteger()

            val hubAccount = HubAccount(
                CryptoService.generateCryptoKey().keyPair.public,
                update,
                allotment,
                deposit,
                withdraw
            )
            if (MockUtils.nextBoolean()) {
                for (i in 1..MockUtils.nextInt(2, 10)) {
                    hubAccount.transactions.add(OffchainTransaction.mock())
                }
            }
            return hubAccount
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.HubAccount): HubAccount {
            return HubAccount(
                CryptoService.loadPublicKey(protoMessage.publicKey.toByteArray()),
                Update.parseFromProtoMessage(protoMessage.update),
                protoMessage.allotment.toByteArray().toBigInteger(),
                protoMessage.deposit.toByteArray().toBigInteger(),
                BigInteger(protoMessage.withdraw.toByteArray()),
                protoMessage.transactionsList.stream().map { OffchainTransaction.parseFromProtoMessage(it) }.collect(
                    Collectors.toList()
                )
            )
        }

        override fun toProtoMessage(obj: HubAccount): Starcoin.HubAccount {
            return Starcoin.HubAccount.newBuilder()
                .setPublicKey(CryptoService.encodePublicKey(obj.publicKey).toByteString())
                .setUpdate(Update.toProtoMessage(obj.update))
                .setAllotment(obj.allotment.toByteArray().toByteString())
                .setDeposit(obj.deposit.toByteArray().toByteString())
                .setWithdraw(obj.withdraw.toByteArray().toByteString())
                .addAllTransactions(
                    obj.transactions.stream().map { OffchainTransaction.toProtoMessage(it) }.collect(
                        Collectors.toList()
                    )
                )
                .build()
        }
    }
}
