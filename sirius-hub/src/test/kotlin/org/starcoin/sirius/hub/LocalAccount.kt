package org.starcoin.sirius.hub

import io.grpc.inprocess.InProcessChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.sirius.channel.EventBus
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import java.security.PublicKey

sealed class LocalAccountAction {
    data class NewTxAction(val tx: OffchainTransaction) : LocalAccountAction()
    data class NewUpdateAction(val update: Update) : LocalAccountAction()
    data class NewHubRoot(val hubRoot: HubRoot) : LocalAccountAction()
}

enum class AccountStatus {
    Init,
    Connected,
    Synced,
    Inconsistent
}

class LocalAccount<T : ChainTransaction, A : ChainAccount>(
    val chainAccount: A,
    val chain: Chain<T, out Block<T>, A>,
    val contract: HubContract<A>,
    val owner: PublicKey,
    val hubService: HubService
) {

    constructor(chainAccount: A, chain: Chain<T, out Block<T>, A>, contract: HubContract<A>, owner: A) : this(
        chainAccount,
        chain,
        contract,
        owner.key.keyPair.public,
        HubServiceImpl(owner, chain, contract)
    )

    constructor(
        chainAccount: A,
        chain: Chain<T, out Block<T>, A>,
        contract: HubContract<A>,
        owner: PublicKey,
        configuration: Configuration
    ) : this(
        chainAccount, chain, contract, owner, HubServiceStub(
            HubServiceGrpc.newBlockingStub(
                InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build()
            )
        )
    )

    val key: CryptoKey = chainAccount.key
    val address = chainAccount.address
    val participant: Participant = Participant(chainAccount.key.keyPair.public)
    internal var state: LocalEonState? = null
    private val eventBus = EventBus<HubEvent>()

    var hubAccount: HubAccount?
        get() = if (this.state == null) null else this.state!!.hubAccount
        set(hubAccount) {
            this.state!!.hubAccount = hubAccount
        }

    var update: Update
        get() = this.state!!.update
        set(value) {
            if (this.state?.update != null) {
                LOG.info("Set $address update")
                assert(value.verifyHubSig(owner))
                assert(this.state!!.update.sign == value.sign)
                this.state!!.update = value
            } else {
                this.state!!.update = value
            }
        }

    var proof: AMTreeProof
        get() = this.state!!.proof!!
        set(value) {
            this.state!!.proof = value
        }

    val txs: List<OffchainTransaction>
        get() = this.state!!.txs

    val isRegister: Boolean
        get() = this.hubAccount != null

    var accountStatus: AccountStatus = AccountStatus.Init

    private var hubEventJob: Job? = null

    private val localActor = GlobalScope.actor<LocalAccountAction> {
        consumeEach {
            when (it) {
                is LocalAccountAction.NewTxAction -> {
                    addTx(it.tx)
                    val toIOU = IOU(it.tx, update)
                    hubService.receiveNewTransfer(toIOU)
                }
                is LocalAccountAction.NewUpdateAction -> {
                    LOG.info("$address process NewUpdateAction ${it.update}")
                    update = it.update
                    hubAccount = hubService.getHubAccount(address)
                }
                is LocalAccountAction.NewHubRoot -> {
                    val hubRoot = it.hubRoot
                    val eon = hubRoot.eon
                    if (state?.eon == eon) {
                        LOG.warning("Current eon root")
                    } else if (state?.eon == eon - 1) {
                        val update = Update(eon, 0, 0, 0)
                        update.sign(key)
                        state = LocalEonState(state, eon, update)
                        LOG.info("$address to new eon: $eon")
                    } else {
                        LOG.warning("Unexpected eon status, localEon: ${state?.eon}, hubRootEon: ${hubRoot.eon}")
                    }
                    state!!.hubRoot = hubRoot
                    state!!.proof = hubService.getProof(address)
                    state!!.hubAccount = hubService.getHubAccount(address)
                    if (!AMTree.verifyMembershipProof(
                            state?.hubRoot?.root,
                            state?.proof
                        )
                    ) {
                        accountStatus = AccountStatus.Inconsistent
                    } else {
                        accountStatus = AccountStatus.Inconsistent
                    }
                }
            }
        }
    }

    fun addTx(tx: OffchainTransaction) {
        this.state!!.addTx(tx)
        val newUpdate = Update.newUpdate(
            this.state!!.eon,
            this.update.version + 1,
            this.participant.address,
            this.txs
        )
        newUpdate.sign(key)
        this.state!!.update = newUpdate
    }

    fun newTx(to: Address, amount: BigInteger): OffchainTransaction {
        val tx = OffchainTransaction(this.state!!.eon, this.address, to, amount)
        this.addTx(tx)
        return tx
    }

    fun init() {
        val currentBlockNumber = chain.getBlockNumber()
        val contractHubInfo = this.contract.queryHubInfo(this.chainAccount)
        val blocksPerEon = contractHubInfo.blocksPerEon
        val startBlockNumber = contractHubInfo.startBlockNumber.longValueExact()
        val currentEon = Eon.calculateEon(startBlockNumber, currentBlockNumber, blocksPerEon)
        val update = Update(currentEon.id, 0, 0, 0)
        update.sign(key)
        this.state = LocalEonState(this.state, currentEon.id, update)
        val updateReturn =
            hubService.registerParticipant(participant, update)
        this.update = updateReturn
        this.hubAccount = hubService.getHubAccount(this.address)
        this.hubEventJob = GlobalScope.launch(Dispatchers.IO) {
            val channel = hubService.watch(address)
            for (event in channel) {
                LOG.info("$address onEvent: $event")
                when (event.type) {
                    HubEventType.NEW_TX -> localActor.send(LocalAccountAction.NewTxAction(event.getPayload()))
                    HubEventType.NEW_UPDATE -> localActor.send(LocalAccountAction.NewUpdateAction(event.getPayload()))
                    HubEventType.NEW_HUB_ROOT -> localActor.send(LocalAccountAction.NewHubRoot(event.getPayload()))
                    else -> LOG.info("un handle event:$event")
                }
                eventBus.send(event)
            }
        }
    }

    private fun processHubEvents() {

    }

    fun watch(filter: (HubEvent) -> Boolean = { true }): ReceiveChannel<HubEvent> {
        return this.eventBus.subscribe(filter)
    }


//    suspend fun deposit(amount: BigInteger) {
//        val previousAccount = hubService.getHubAccount(a.address)!!
//        // deposit
//
//        val hubEventFuture =
//            HubEventFuture { event -> event.type === HubEventType.NEW_DEPOSIT && event.address == a.address }
//        a.watch(hubEventFuture)
//
//        val txDeferred = chain.submitTransaction(
//            a.chainAccount,
//            chain.newTransaction(a.chainAccount, contract.contractAddress, amount)
//        )
//
//        val receipt = txDeferred.awaitTimout()
//        Assert.assertTrue(receipt.status)
//
//        try {
//            val hubEvent = hubEventFuture.get(4, TimeUnit.SECONDS)
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
//        val hubAccount = hubService.getHubAccount(a.address)!!
//        Assert.assertEquals(
//            if (expectSuccess) previousAccount.deposit + amount else previousAccount.deposit,
//            hubAccount.deposit
//        )
//        a.hubAccount = hubAccount
//    }

    fun destroy() {
        this.hubEventJob?.cancel()
    }

    companion object : WithLogging() {

    }
}
