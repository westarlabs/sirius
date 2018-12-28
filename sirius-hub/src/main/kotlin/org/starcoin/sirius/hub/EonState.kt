package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.Eon.Epoch
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EonState(val eon: Int, val previous: EonState? = null) {
    private val accounts: MutableList<HubAccount>
    var state: AMTree
        private set
    var currentEpoch: Epoch = Eon.Epoch.FIRST
        private set
    private val senderIOUs: MutableMap<Address, IOU>
    private val receiverIOUs: MutableMap<Address, IOU>

    init {
        this.accounts = ArrayList()
        if (this.previous != null) {
            this.previous.getAccounts().forEach { account -> addAccount(account.toNextEon(this.eon)) }
            val accounts = this.previous
                .getAccounts()
            this.state = AMTree(this.eon, accounts)
        } else {
            this.state = AMTree(this.eon, ArrayList())
        }
        this.senderIOUs = ConcurrentHashMap()
        this.receiverIOUs = ConcurrentHashMap()
    }

    fun getAccount(address: Address): Optional<HubAccount> {
        return this.getAccount { hubAccount -> hubAccount.address == address }
    }

    fun getAccount(predicate: (HubAccount) -> Boolean): Optional<HubAccount> {
        return accounts.stream().filter(predicate).findFirst()
    }

    fun addAccount(account: HubAccount) {
        this.accounts.add(account)
    }

    fun getAccounts(): List<HubAccount> {
        return Collections.unmodifiableList(this.accounts)
    }

    fun setEpoch(epoch: Epoch) {
        if (currentEpoch !== epoch) {
            this.currentEpoch = epoch
        }
    }

    fun addIOU(iou: IOU) {
        this.senderIOUs[iou.transaction.from] = iou
        this.receiverIOUs[iou.transaction.to] = iou
    }

    fun getIOUByFrom(blockAddress: Address): IOU? {
        return this.senderIOUs[blockAddress]
    }

    fun getIOUByTo(blockAddress: Address): IOU? {
        return this.receiverIOUs[blockAddress]
    }

    fun removeIOU(iou: IOU) {
        this.senderIOUs.remove(iou.transaction.from)
        this.receiverIOUs.remove(iou.transaction.to)
    }
}
