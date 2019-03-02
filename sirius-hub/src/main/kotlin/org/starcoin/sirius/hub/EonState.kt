package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.Eon.Epoch
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EonState(val eon: Int, val previous: EonState? = null) {
    var state: AMTree
        private set
    var currentEpoch: Epoch = Eon.Epoch.FIRST
        private set
    private val senderIOUs: MutableMap<Address, IOU>
    private val receiverIOUs: MutableMap<Address, IOU>
    private val factory: DataStoreFactory

    private val hubAccountStore: HubAccountStore

    init {
        if (this.previous != null) {
            this.state = AMTree(this.eon, this.previous.hubAccountStore.asHubAccountIterable())
            this.factory = previous.factory
        } else {
            this.state = AMTree(this.eon, ArrayList())
            //TODO
            this.factory = MapDataStoreFactory()
        }
        this.hubAccountStore = HubAccountStore(eon, factory)
        this.senderIOUs = ConcurrentHashMap()
        this.receiverIOUs = ConcurrentHashMap()
        if (this.previous != null) {
            this.previous.forEach { account -> addAccount(account.toNextEon(this.eon)) }
        }
    }

    fun getAccount(address: Address): HubAccount? {
        return this.hubAccountStore.get(address)
    }

    fun getAccount(predicate: (HubAccount) -> Boolean): HubAccount? {
        return this.hubAccountStore.asHubAccountIterable().firstOrNull(predicate)
    }

    fun addAccount(account: HubAccount) {
        this.hubAccountStore.put(account)
    }


    fun forEach(consumer: (HubAccount) -> Unit) {
        this.hubAccountStore.asHubAccountIterable().forEach(consumer)
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

    fun saveAccount(account: HubAccount) {
        this.hubAccountStore.put(account)
    }
}
