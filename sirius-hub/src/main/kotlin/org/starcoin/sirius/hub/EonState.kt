package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.Eon.Epoch
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import org.starcoin.sirius.lang.settableLazy

class EonState(val eon: Int, private val factory: DataStoreFactory = MapDataStoreFactory()) {

    var currentEpoch: Epoch = Eon.Epoch.FIRST
        private set
    private val accountStore: HubAccountStore = HubAccountStore(eon, factory)

    var previous: EonState? by settableLazy {
        when (eon) {
            0 -> null
            else -> EonState(eon - 1, factory)
        }
    }

    //TODO store tree's nodes hash and implement lazy load tree.
    val state: AMTree by lazy {
        this.previous?.let { AMTree(eon, it.accountStore.asHubAccountIterable()) } ?: AMTree(eon, listOf())
    }

    private constructor(eon: Int, previous: EonState) : this(
        eon,
        previous.factory
    ) {
        this.previous = previous
        this.accountStore.updateBatch(previous.accountStore.asHubAccountIterable().map { it.toNextEon(eon) })
    }

    fun getAccountOrNull(address: Address): HubAccount? {
        return this.accountStore.get(address)
    }

    fun getAccountOrNull(predicate: (HubAccount) -> Boolean): HubAccount? {
        return this.accountStore.asHubAccountIterable().firstOrNull(predicate)
    }

    fun getAccount(address: Address): HubAccount {
        return this.getAccountOrNull(address) ?: fail(Status.NOT_FOUND) { "Can not find account by address: $address" }
    }

    fun addAccount(account: HubAccount) {
        this.accountStore.put(account)
    }


    fun forEach(consumer: (HubAccount) -> Unit) {
        this.accountStore.asHubAccountIterable().forEach(consumer)
    }

    fun setEpoch(epoch: Epoch) {
        if (currentEpoch !== epoch) {
            this.currentEpoch = epoch
        }
    }

    fun addIOU(iou: IOU) {
        val from = this.getAccountOrNull(iou.transaction.from)!!
        val to = this.getAccountOrNull(iou.transaction.to)!!
        from.appendSendTx(iou)
        to.appendReceiveTx(iou.transaction)
        this.saveAccount(from)
        this.saveAccount(to)
    }

    fun getPendingSendTx(address: Address): IOU? {
        return this.getAccountOrNull(address)?.getPendingSendTx()
    }

    fun getPendingReceiveTxs(address: Address): List<OffchainTransaction> {
        return this.getAccountOrNull(address)?.getPendingReceiveTxs() ?: emptyList()
    }

    fun saveAccount(account: HubAccount) {
        this.accountStore.put(account)
    }

    fun toNextEon(): EonState {
        val newEon = eon + 1
        return EonState(newEon, this)
    }

}
