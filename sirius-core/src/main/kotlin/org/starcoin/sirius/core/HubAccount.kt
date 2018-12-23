package org.starcoin.sirius.core

import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import org.starcoin.proto.Starcoin.ProtoHubAccount
import org.starcoin.sirius.util.KeyPairUtil
import java.security.PublicKey
import java.util.*

class HubAccount : ProtobufCodec<ProtoHubAccount> {

    var address: Address? = null
        private set
    var allotment: Long = 0
        private set
    var update: Update? = null
        private set
    var deposit: Long = 0
        private set
    var withdraw: Long = 0
        private set
    private val transactions: MutableList<OffchainTransaction> = mutableListOf()
    var publicKey: PublicKey? = null
        private set

    val balance: Long
        get() = ((this.allotment
                + deposit
                + update!!.receiveAmount)
                - this.withdraw
                - update!!.sendAmount)

    constructor() {}

    constructor(hubAccount: ProtoHubAccount) {
        this.unmarshalProto(hubAccount)
    }

    constructor(address: Address, allotment: Long, update: Update, publicKey: PublicKey) {
        this.address = address
        this.allotment = allotment
        this.update = update
        this.publicKey = publicKey
    }

    fun appendTransaction(tx: OffchainTransaction, update: Update) {
        this.checkUpdate(tx, update)
        this.transactions.add(tx)
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
        val prepareUpdate = Update(newUpdate.eon, newUpdate.version, this.address!!, sendTxs)

        Preconditions.checkArgument(
            newUpdate.root == prepareUpdate.root,
            "check " + this.address + " update root hash fail, expect:" + prepareUpdate.root!!
                .toMD5Hex() + ", but get " + newUpdate.root!!.toMD5Hex()
        )

        Preconditions.checkArgument(
            newUpdate.sendAmount == prepareUpdate.sendAmount, "sendAmount"
        )
        Preconditions.checkArgument(
            newUpdate.receiveAmount == prepareUpdate.receiveAmount,
            String.format(
                "expect receiveAmount %s, but get %s",
                prepareUpdate.receiveAmount, newUpdate.receiveAmount
            )
        )

        Preconditions.checkState(this.update != null, "previousUpdate")
        Preconditions.checkArgument(newUpdate.version > update!!.version)
        Preconditions.checkArgument(checkBalance(), "has not enough balance.")
    }

    private fun checkBalance(): Boolean {
        return this.balance >= 0
    }

    fun toNewAccountInformation(): AccountInformation {
        val allotment = this.calculateNewAllotment()
        return AccountInformation(address!!, allotment, update)
    }

    private fun calculateNewAllotment(): Long {
        val allotment = this.balance
        assert(allotment >= 0)
        return allotment
    }

    fun toNextEon(eon: Int): HubAccount {
        val allotment = this.calculateNewAllotment()
        return HubAccount(address!!, allotment, Update(eon, 0, 0, 0, null), publicKey!!)
    }

    override fun marshalProto(): ProtoHubAccount {
        return ProtoHubAccount.newBuilder()
            .setAddress(this.address!!.toByteString())
            .setUpdate(this.update!!.toProto())
            .setAllotment(this.allotment)
            .setDeposit(this.deposit)
            .setWithdraw(this.withdraw)
            .setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey!!)))
            .build()
    }

    override fun unmarshalProto(proto: ProtoHubAccount) {
        this.address = Address.wrap(proto.address)
        this.update = Update(proto.update)
        this.allotment = proto.allotment
        this.deposit = proto.deposit
        this.withdraw = proto.withdraw
        this.publicKey = KeyPairUtil.recoverPublicKey(proto.publicKey.toByteArray())
    }

    override fun toString(): String {
        return this.toJson()
    }
}
