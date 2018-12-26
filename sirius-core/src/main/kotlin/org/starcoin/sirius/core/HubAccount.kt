package org.starcoin.sirius.core

import com.google.common.base.Preconditions
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin.ProtoHubAccount
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.PublicKeySerializer
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import java.security.PublicKey
import java.util.*
import java.util.stream.Collectors

@Serializable
@ProtobufSchema(ProtoHubAccount::class)
data class HubAccount(
    @Serializable(with = PublicKeySerializer::class)
    @SerialId(1)
    val publicKey: PublicKey,
    @SerialId(2)
    var update: Update,
    @SerialId(3)
    var allotment: Long = 0,
    @SerialId(4)
    var deposit: Long = 0,
    @SerialId(5)
    var withdraw: Long = 0,
    @SerialId(6)
    @Optional
    private val transactions: MutableList<OffchainTransaction> = mutableListOf()
) : SiriusObject() {
    init {
        assert(checkBalance())
    }

    @Transient
    val address = Address.getAddress(publicKey)

    @Transient
    val balance: Long
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

    fun addDeposit(amount: Long) {
        this.deposit = this.deposit + amount
    }

    fun addWithdraw(amount: Long): Boolean {
        if (this.balance - amount >= 0) {
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
        return this.balance >= 0
    }

    fun calculateNewAllotment(): Long {
        val allotment = this.balance
        assert(allotment >= 0)
        return allotment
    }

    fun toNextEon(eon: Int): HubAccount {
        val allotment = this.calculateNewAllotment()
        return HubAccount(publicKey, Update(UpdateData(eon, 0, 0, 0)), allotment)
    }

    companion object : SiriusObjectCompanion<HubAccount, ProtoHubAccount>(HubAccount::class) {
        override fun mock(): HubAccount {
            val update = Update.mock()
            val deposit = MockUtils.nextLong()
            val withdraw = MockUtils.nextLong()
            val allotment = MockUtils.nextLong(
                Math.abs(update.receiveAmount + deposit - withdraw - update.sendAmount),
                Long.MAX_VALUE / 2
            )
            val hubAccount = HubAccount(
                CryptoService.generateCryptoKey().getKeyPair().public,
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

        override fun parseFromProtoMessage(protoMessage: ProtoHubAccount): HubAccount {
            return HubAccount(
                CryptoService.loadPublicKey(protoMessage.publicKey.toByteArray()),
                Update.parseFromProtoMessage(protoMessage.update),
                protoMessage.allotment,
                protoMessage.deposit,
                protoMessage.withdraw,
                protoMessage.transactionsList.stream().map { OffchainTransaction.parseFromProtoMessage(it) }.collect(
                    Collectors.toList()
                )
            )
        }

        override fun toProtoMessage(obj: HubAccount): ProtoHubAccount {
            return ProtoHubAccount.newBuilder()
                .setPublicKey(CryptoService.encodePublicKey(obj.publicKey).toByteString())
                .setUpdate(Update.toProtoMessage(obj.update))
                .setAllotment(obj.allotment)
                .setDeposit(obj.deposit)
                .setWithdraw(obj.withdraw)
                .addAllTransactions(
                    obj.transactions.stream().map { OffchainTransaction.toProtoMessage(it) }.collect(
                        Collectors.toList()
                    )
                )
                .build()
        }
    }
}
