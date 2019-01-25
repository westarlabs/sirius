package org.starcoin.sirius.hub

import com.google.common.eventbus.EventBus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class LocalAccount<T : ChainTransaction, A : ChainAccount>(
    val chainAccount: A,
    val chain: Chain<T, out Block<T>, A>,
    val contract: HubContract<A>,
    val hubService: HubService
) {

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
        this.state!!.proof = hubService.getProof(this.address)
        Assert.assertTrue(
            AMTree.verifyMembershipProof(
                this.state?.hubRoot?.root,
                this.state?.proof
            )
        )

        this.state!!.hubAccount = hubService.getHubAccount(this.address)
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
        this.processHubEvents()
    }

    private fun processHubEvents() {
        GlobalScope.launch {
            val channel = hubService.watch(address)
            for (event in channel) {
                LOG.info("$address onEvent: $event")
                if (event.type === HubEventType.NEW_TX) {
                    onTx(event.getPayload() as OffchainTransaction)
                } else if (event.type === HubEventType.NEW_UPDATE) {
                    onUpdate(event.getPayload() as Update)
                }
                eventBus.post(event)
            }
        }
    }

    fun watch(future: HubEventFuture) {
        this.eventBus.register(future)
    }

    fun onTx(tx: OffchainTransaction) {
        this.addTx(tx)
        val toIOU = IOU(tx, this.update)
        hubService.receiveNewTransfer(toIOU)
    }

    fun onUpdate(update: Update) {
        this.update = update
        this.hubAccount = hubService.getHubAccount(this.address)
    }

    fun deposit(amount: BigInteger) {
        val previousAccount = hubService.getHubAccount(address)
        // deposit

        val hubEventFuture =
            HubEventFuture { event -> event.type === HubEventType.NEW_DEPOSIT && event.address == address }
        watch(hubEventFuture)

        val txHash = chain.submitTransaction(
            chainAccount,
            chain.newTransaction(chainAccount, contract.contractAddress, amount)
        )

        //val future = this.registerTxHook(txHash)
        //future.get(4, TimeUnit.SECONDS)

//        try {
//            val hubEvent = hubEventFuture.get(2, TimeUnit.SECONDS)
//            if (expectSuccess) {
//                Assert.assertEquals(amount, hubEvent.getPayload<Deposit>().amount)
//            } else {
//                Assert.fail("expect get Deposit event timeout")
//            }
//        } catch (e: Exception) {
//            if (expectSuccess) {
//                Assert.fail(e.message)
//            }
//        }
//        //TODO ensure
//        HubServerIntegrationTestBase.sleep(1000)
//        val hubAccount = HubAccount.parseFromProtoMessage(hubService.getHubAccount(a.address.toProto()))
//        Assert.assertEquals(
//            if (expectSuccess) previousAccount.deposit + amount else previousAccount.deposit,
//            hubAccount.deposit
//        )
//        a.hubAccount = hubAccount
    }

    companion object : WithLogging() {

    }
}
