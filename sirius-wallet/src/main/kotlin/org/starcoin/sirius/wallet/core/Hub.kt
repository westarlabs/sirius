package org.starcoin.sirius.wallet.core

import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.wallet.core.store.Store
import java.lang.RuntimeException
import java.math.BigInteger

import kotlin.properties.Delegates

class Hub <T : ChainTransaction, A : ChainAccount> {

    companion object : WithLogging()

    private var contract: HubContract<A>  by Delegates.notNull()

    private var account: A  by Delegates.notNull()

    private var currentEon: Eon  by Delegates.notNull();

    private var channelManager: ChannelManager by Delegates.notNull()

    private var serverEventHandler: ServerEventHandler?

    private var hubAddr: String by Delegates.notNull()

    private var hubAccount: HubAccount? = null

    private var dataStore: Store<HubStatus>?

    internal var hubStatus: HubStatus by Delegates.notNull()
        private set

    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    internal var eonChannel :Channel<ClientEventType>? = null

    // for test lost connect
    var disconnect = true

    var alreadWatch = false

    constructor(
        contract: HubContract<A>,
        account: A,
        channelManager: ChannelManager,
        serverEventHandler: ServerEventHandler?,
        eonStatusStore: Store<HubStatus>?,
        chain :Chain<T, out Block<T>, A>
    ) {
        this.contract = contract
        this.account = account
        this.channelManager = channelManager
        this.serverEventHandler = serverEventHandler
        this.dataStore = eonStatusStore
        this.chain = chain

        var hubInfo=contract.queryHubInfo(account)
        hubAddr=hubInfo.hubAddress

        this.currentEon=Eon.calculateEon(blocksPerEon = hubInfo.blocksPerEon,blockHeight = chain.getBlockNumber().toLong())

        this.hubStatus = HubStatus(this.currentEon)
        hubStatus.blocksPerEon= hubInfo.blocksPerEon

    }

    private fun getChainEon():Eon{
        var hubInfo=contract.queryHubInfo(account)
        return Eon.calculateEon(blocksPerEon = hubInfo.blocksPerEon,blockHeight = chain.getBlockNumber().toLong())
    }

    fun onHubRootCommit(hubRoot: HubRoot) {
        try {
            // System.out.println("get new hub root"+hubRoot.toString());
            LOG.info("start get eon")
            val eon = this.getChainEon()
            LOG.info("finish get eon")

            /**
            if (this.accountInfo() == null) {
                return
            }**/
            LOG.info("current eon is "+eon.id+" hubroot eon is " +hubRoot.eon)
            if (eon.id === hubRoot.eon) {
                LOG.info("start change eon")
                nextEon(hubRoot)
                LOG.info("finish change eon")
            } else {
                throw RuntimeException("hub eon is not right")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun onDeposit(deposit: Deposit) {
        serverEventHandler?.onDeposit(deposit)
    }

    internal fun onWithdrawal(withdrawalStatus: WithdrawalStatus) {
        var clientEventType = ClientEventType.INIT_WITHDRAWAL
        when (withdrawalStatus.status) {
            WithdrawalStatusType.INIT.number -> {
                this.hubStatus.syncWithDrawal(withdrawalStatus)
                clientEventType = ClientEventType.INIT_WITHDRAWAL
            }
            WithdrawalStatusType.CANCEL.number -> {
                this.hubStatus.cancelWithDrawal()
                LOG.info("cancel")
            }
            WithdrawalStatusType.PASSED.number -> {
                this.hubStatus.syncWithDrawal(withdrawalStatus)
                LOG.info("pass")
            }
            else -> LOG.info(withdrawalStatus.toJSON()
            )
        }
        GlobalScope.launch {
            eonChannel?.send(clientEventType)
        }
        serverEventHandler?.onWithdrawal(withdrawalStatus)
    }

    private fun onNewTransaction(offchainTransaction: OffchainTransaction) {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        this.hubStatus.addOffchainTransaction(offchainTransaction)

        val recieveUpdate = Update.newUpdate(
            this.currentEon.id,
            this.hubStatus.currentUpdate(currentEon).version + 1,
            account.address,
            this.hubStatus.currentTransactions()
        )
        recieveUpdate.sign(account.key)

        val iou = IOU(offchainTransaction, recieveUpdate)

        val succResponse = hubServiceBlockingStub.receiveNewTransfer(iou.toProto())
        LOG.info("recieve new transfer from " + offchainTransaction.from + succResponse)

        serverEventHandler?.onNewTransaction(offchainTransaction)

    }

    fun recieveTransacion() {

    }

    fun recieveHubSign() {
    }

    private fun onNewUpdate(update: Update) {
        this.hubStatus.addUpdate(update)
        serverEventHandler?.onNewUpdate(update)
        LOG.info("get hub sign")
    }

    @Synchronized
    private fun nextEon(hubRoot: HubRoot) {
        this.currentEon = this.getChainEon()

        this.checkChallengeStatus()
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(this.channelManager.hubChannel)

        val protoAugmentedMerkleProof = hubServiceBlockingStub.getProof(account.address.toProto())
        val proof = AMTreeProof.parseFromProtoMessage(protoAugmentedMerkleProof)

        val result = AMTree.verifyMembershipProof(hubRoot.root, proof)
        var needChallenge = false
        if (result == false) {
            LOG.info("hub server lie")
            needChallenge = true
        }else{
            LOG.info("verify hub commit pass!!")
        }

        val lastUpdate = hubStatus.currentUpdate(this.currentEon)

        // 加上已经确认的转进来的钱,加上别人转过来的钱，减去转给别人的钱
        val lastIndex = this.hubStatus.nextEon(this.currentEon, proof)

        val selfNode = proof.path.leafNode

        if (selfNode.allotment < this.getAvailableCoin()) {
            needChallenge = true
            LOG.info("proof hub allot is " + selfNode.allotment)
            LOG.info("local allot is " + this.getAvailableCoin())
        }else {
            LOG.info("challenge is not necessary")
        }
        //this.dataStore?.save(this.hubStatus)

        //openBalanceUpdateChallenge(lastUpdate, lastIndex)
        GlobalScope.launch {
            eonChannel?.send(ClientEventType.FINISH_EON_CHANGE)
        }

        if (!needChallenge) {
            return
        }

    }

    internal fun openBalanceUpdateChallenge(){
        val balanceUpdateProof = BalanceUpdateProof(this.hubStatus.currentUpdate(this.currentEon),this.hubStatus.currentEonProof())
        var challenge=BalanceUpdateChallenge(balanceUpdateProof,account.key.keyPair.public)
        this.contract.openBalanceUpdateChallenge(account,challenge)
    }

    fun getAvailableCoin():BigInteger {
        return BigInteger.valueOf(0)
    }

    fun getWithdrawalCoin():Long {
        return 0;
    }

    @Synchronized
    fun sync() {
    }

    fun newTransfer(addr:Address, value:Long) :OffchainTransaction{
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        val tx = OffchainTransaction(this.currentEon.id, account.address, addr, value)
        tx.sign(account.key)
        this.hubStatus.addOffchainTransaction(tx)

        val update = Update.newUpdate(
            this.currentEon.id,
            this.hubStatus.currentUpdate(currentEon).version + 1,
            addr,
            this.hubStatus.currentTransactions()
        )
        update.sign(account.key)

        val iou = IOU(tx, update)

        val succResponse = hubServiceBlockingStub.sendNewTransfer(iou.toProto())
        //dataStore.save(this.hubStatusData)
        return if (succResponse.getSucc() == true) {
            tx
        } else {
            throw RuntimeException("offlien transfer failed")
        }
    }

    fun deposit(value :Long) {
        var chainTransaction=chain.newTransaction(account,contract.contractAddress, BigInteger.valueOf(value))
        var hash=chain.submitTransaction(account,chainTransaction)
        this.hubStatus.addDepositTransaction(hash,chainTransaction)
    }

    fun register() : Update? {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        val builder = Starcoin.RegisterParticipantRequest.newBuilder()
        val participant = Participant(account.key.keyPair.public)

        builder.setParticipant(Participant.toProtoMessage(participant))
        val update = Update(getChainEon().id, 0, BigInteger.valueOf(0), BigInteger.valueOf(0), Hash.EMPTY_DADA_HASH)
        update.sign(this.account.key)
        builder.setUpdate(Update.toProtoMessage(update))

        try {
            val updateResponse = hubServiceBlockingStub.registerParticipant(builder.build())
            val response = Update.parseFromProtoMessage(updateResponse)
            hubStatus.addUpdate(response)
            this.accountInfo()
            watchHubEnvent()
            this.disconnect = false
            return response
        } catch (e: StatusRuntimeException) {
            throw e
        }

    }

    fun  accountInfo():HubAccount ?{
        return hubAccount
    }

    fun openTransferChallenge(transactionHash:Hash) :Boolean?{
        return true
    }

    fun newTransferChallenge (
        update: Update, path: MerkleTree, transaction: OffchainTransaction
    ): ChainTransaction? {
        return null
    }

    fun withDrawal(value: Long) {
        if (!hubStatus.couldWithDrawal()) {
            LOG.info("already have withdrawal in progress.")
            return
        }
        if (hubStatus.currentEonProof() == null) {
            LOG.info("last eon path doesn't exists.")
            return
        }

        val withdrawal = Withdrawal(hubStatus.currentEonProof()!!, value)
        this.contract.initiateWithdrawal(account,withdrawal)
    }

    fun cheat(flag:Int){

    }

    fun restore() {
    }

    private fun syncBlocks() {
    }

    private fun checkChallengeStatus() {
    }

    internal fun confirmDeposit(transaction: ChainTransaction){
        this.hubStatus.confirmDeposit(transaction)
    }

    internal fun getBalance():BigInteger{
        return hubStatus.allotment
    }

    @Synchronized
    private fun watchHubEnvent() {
        if (alreadWatch == true) {
            return
        }

        val hubServiceStub = HubServiceGrpc.newStub(this.channelManager.hubChannel)

        val hubObserver = HubObserver()
        hubObserver.addConsumer(this::hubRootConsumer)

        hubServiceStub.watch(account.address.toProto(), hubObserver)
        this.alreadWatch = true
    }

    private fun hubRootConsumer(value: Starcoin.HubEvent) {
        if (disconnect) {
            return
        }
        try {
            var clientEvent = ClientEventType.NOTHING
            when (value.type.number) {
                Starcoin.HubEventType.HUB_EVENT_NEW_TX_VALUE -> {
                    onNewTransaction(OffchainTransaction.parseFromProtobuf(value.payload.toByteArray()))
                    clientEvent= ClientEventType.NEW_OFFLINE_TRANSACTION
                }
                Starcoin.HubEventType.HUB_EVENT_NEW_UPDATE_VALUE -> {
                    onNewUpdate(Update.parseFromProtobuf(value.payload.toByteArray()))
                    clientEvent= ClientEventType.HUB_SIGN
                }else -> {
            }
            }
            GlobalScope.launch {
                eonChannel?.send(clientEvent)
            }
            //dataStore.save(this.hubStatusData)
        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
        }
    }

}
