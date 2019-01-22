package org.starcoin.sirius.hub

import com.google.common.eventbus.EventBus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class LocalAccount<A : ChainAccount>(val chainAccount: A, val hubService: HubServiceGrpc.HubServiceBlockingStub) {

    val kp: CryptoKey = chainAccount.key
    val address = chainAccount.address
    val p: Participant = Participant(chainAccount.key.keyPair.public)
    internal var state: LocalEonState? = null
    private val eventBus = EventBus()

    var hubAccount: HubAccount?
        get() = if (this.state == null) null else this.state!!.hubAccount
        set(hubAccount) {
            this.state!!.hubAccount = hubAccount
        }

    var update: Update
        get() = this.state!!.update
        set(update) {
            this.state!!.update = update
        }

    val txs: List<OffchainTransaction>
        get() = this.state!!.txs

    val isRegister: Boolean
        get() = this.hubAccount != null

    fun onEon(hubRoot: HubRoot) {
        val eon = hubRoot.eon
        val update = Update(eon, 0, 0, 0)
        update.sign(kp)
        this.state = LocalEonState(this.state, eon, update)
        this.state!!.hubRoot = hubRoot
        this.state!!.proof = AMTreeProof.parseFromProtoMessage(hubService.getProof(this.address.toProto()))
        Assert.assertTrue(
            AMTree.verifyMembershipProof(
                this.state?.hubRoot?.root,
                this.state?.proof
            )
        )

        this.state!!.hubAccount =
            HubAccount.parseFromProtoMessage(hubService.getHubAccount(this.address.toProto()))
        LOG.info(this.address.toString() + " to new eon:" + eon)
    }

    fun initUpdate(eon: Int): Update {
        val update = Update(eon, 0, 0, 0)
        update.sign(kp)
        this.state = LocalEonState(this.state, eon, update)
        return update
    }

    fun addTx(tx: OffchainTransaction) {
        this.state!!.addTx(tx)
        val newUpdate = Update.newUpdate(
            this.state!!.eon,
            this.update.version + 1,
            this.p.address,
            this.txs
        )
        newUpdate.sign(kp)
        this.state!!.update = newUpdate
    }

    fun newTx(to: Address, amount: BigInteger): OffchainTransaction {
        val tx = OffchainTransaction(this.state!!.eon, this.address, to, amount)
        this.addTx(tx)
        return tx
    }

    fun register(update: Update, hubAccount: HubAccount) {
        this.state!!.update = update
        this.state!!.hubAccount = hubAccount
        GlobalScope.launch {
            try {
                //executorService.submit {
                hubService
                    .watch(address.toProto())
                    .forEachRemaining { protoHubEvent ->
                        val event = HubEvent.parseFromProtoMessage(protoHubEvent)
                        HubServerIntegrationTestBase.LOG.info("onEvent:" + event.toString())
                        if (event.type === HubEventType.NEW_TX) {
                            onTx(event.getPayload() as OffchainTransaction)
                        } else if (event.type === HubEventType.NEW_UPDATE) {
                            onUpdate(event.getPayload() as Update)
                        }
                        eventBus.post(event)
                    }
                //}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun watch(future: HubEventFuture) {
        this.eventBus.register(future)
    }

    fun onTx(tx: OffchainTransaction) {
        this.addTx(tx)
        val toIOU = IOU(tx, this.update)
        Assert.assertTrue(hubService.receiveNewTransfer(toIOU.toProto()).getSucc())
    }

    fun onUpdate(update: Update) {
        this.update = update
        this.hubAccount =
            HubAccount.parseFromProtoMessage(hubService.getHubAccount(this.address.toProto()))
    }

    companion object : WithLogging() {

    }
}
