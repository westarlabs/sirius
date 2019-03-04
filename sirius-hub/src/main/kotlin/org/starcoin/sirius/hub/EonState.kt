package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.Eon.Epoch
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import java.util.*

class EonState(val eon: Int, val previous: EonState? = null) {
    var state: AMTree
        private set
    var currentEpoch: Epoch = Eon.Epoch.FIRST
        private set
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
        val from = this.getAccount(iou.transaction.from)!!
        val to = this.getAccount(iou.transaction.to)!!
        from.appendSendTx(iou)
        to.appendReceiveTx(iou.transaction)
        this.saveAccount(from)
        this.saveAccount(to)
    }

    fun getPendingSendTx(address: Address): IOU? {
        return this.getAccount(address)?.getPendingSendTx()
    }

    fun getPendingReceiveTxs(address: Address): List<OffchainTransaction> {
        return this.getAccount(address)?.getPendingReceiveTxs() ?: emptyList()
    }

    fun saveAccount(account: HubAccount) {
        this.hubAccountStore.put(account)
    }
}
