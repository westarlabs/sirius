package org.starcoin.sirius.wallet.core

import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.Status
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
import java.util.stream.Collectors

import kotlin.properties.Delegates

class Hub <T : ChainTransaction, A : ChainAccount> {

    companion object : WithLogging()

    private var contract: HubContract<A>  by Delegates.notNull()

    private var account: A  by Delegates.notNull()

    internal var currentEon: Eon  by Delegates.notNull()
    private set

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

        this.currentEon=Eon.calculateEon(startBlockNumber = hubInfo.startBlockNumber.toLong(),blocksPerEon = hubInfo.blocksPerEon,currentBlockNumber = chain.getBlockNumber().toLong())

        this.hubStatus = HubStatus(this.currentEon)
        hubStatus.blocksPerEon= hubInfo.blocksPerEon

    }

    private fun getChainEon():Eon{
        var hubInfo=contract.queryHubInfo(account)
        return Eon.calculateEon(startBlockNumber = hubInfo.startBlockNumber.toLong(),blocksPerEon = hubInfo.blocksPerEon,currentBlockNumber = chain.getBlockNumber().toLong())
    }

    @Synchronized
    fun onHubRootCommit(hubRoot: HubRoot) {
        try {
            // System.out.println("get new hub root"+hubRoot.toString());
            LOG.info("start get eon")
            val eon = this.getChainEon()
            LOG.info("finish get eon")

            if (this.accountInfo() == null) {
                return
            }
            LOG.info("current eon is "+eon.id+" hubroot eon is " +hubRoot.eon)
            if (eon.id === hubRoot.eon) {
                LOG.info("start change eon")
                nextEon(hubRoot)
                LOG.info("finish change eon")
            } else {
                GlobalScope.launch { eonChannel?.send(ClientEventType.EON_CHANGE_EXCEPTION) }
                LOG.warning("hub eon is not right")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun onDeposit(deposit: Deposit) {
        serverEventHandler?.onDeposit(deposit)
    }

    internal fun cancelWithdrawal(cancelWithdrawal: CancelWithdrawal){
        this.hubStatus.cancelWithDrawal()
        GlobalScope.launch {
            eonChannel?.send(ClientEventType.CANCEL_WITHDRAWAL)
        }
        LOG.info("cancel withdrawal")
    }

    internal fun onWithdrawal(withdrawalStatus: WithdrawalStatus) {
        var clientEventType = ClientEventType.INIT_WITHDRAWAL
        when (withdrawalStatus.status) {
            WithdrawalStatusType.INIT.number -> {
                this.hubStatus.withdrawalStatus=withdrawalStatus
                clientEventType = ClientEventType.INIT_WITHDRAWAL
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
        LOG.info("IOU is $iou")
        val succResponse = hubServiceBlockingStub.receiveNewTransfer(iou.toProto())
        if(succResponse.succ){
           this.hubStatus.update=recieveUpdate
        }
        LOG.info("recieve new transfer from " + offchainTransaction.from + succResponse)

        serverEventHandler?.onNewTransaction(offchainTransaction)

    }

    fun recieveTransacion() {

    }

    fun recieveHubSign() {
    }

    fun hubInfo():ContractHubInfo{
        return contract.queryHubInfo(account)
    }

    private fun onNewUpdate(update: Update) {
        if(!(this.hubStatus.update?.sign?.equals(update.sign)?:false)){
            return
        }
        this.hubStatus.addUpdate(update)
        serverEventHandler?.onNewUpdate(update)
        LOG.info("get hub sign")
    }

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
        this.hubStatus.nextEon(this.currentEon, proof)

        val selfNode = proof.path.leafNode

        if (selfNode.allotment < this.getAvailableCoin()) {
            needChallenge = true
            LOG.info("proof hub allot is " + selfNode.allotment)
            LOG.info("local allot is " + this.getAvailableCoin())
        }else {
            LOG.info("challenge is not necessary")
        }

        GlobalScope.launch {
            eonChannel?.send(ClientEventType.FINISH_EON_CHANGE)
        }

        if (!needChallenge) {
            return
        }
        var balanceUpdateProof=this.hubStatus.newChallenge(lastUpdate)
        this.contract.openBalanceUpdateChallenge(account,balanceUpdateProof)

        GlobalScope.launch {
            eonChannel?.send(ClientEventType.OPEN_BALANCE_UPDATE_CHALLENGE)
        }

    }

    internal fun getAvailableCoin():BigInteger {
        return hubStatus.getAvailableCoin(this.currentEon)
    }

    fun getWithdrawalCoin():Long {
        return 0;
    }

    @Synchronized
    fun sync() {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        this.currentEon = this.getChainEon()

        this.hubStatus = HubStatus(this.currentEon)

        for (i in 0..2) {
            val eonId = this.currentEon.id - i
            if (eonId < 0) {
                break
            }
            val index = this.hubStatus.getEonByIndex(-i)

            if (this.hubStatus.eonStatuses[index] == null) {
                this.hubStatus.eonStatuses[index] = EonStatus()
            }

            val accountInfo = hubServiceBlockingStub.getHubAccount(account.address.toProto())
            this.hubStatus.eonStatuses[index].updateHistory.add(accountInfo.update.toSiriusObject())
            this.hubStatus.eonStatuses[index].transactionHistory.addAll(
               accountInfo.transactionsList.map {  it.toSiriusObject<Starcoin.OffchainTransaction,OffchainTransaction>() }
            )

            if (i > 0) { // 当前伦次不需要proof

                if (eonId > 0) {
                    val blockAddressAndEonBuilder = Starcoin.BlockAddressAndEon.newBuilder()
                    blockAddressAndEonBuilder.address = account.address.toProto().toByteString()
                    blockAddressAndEonBuilder.eon = eonId

                    val proof = hubServiceBlockingStub.getProofWithEon(blockAddressAndEonBuilder.build())

                    this.hubStatus.eonStatuses[index].treeProof = proof.toSiriusObject()
                }
            } else { // 当前轮次同步余额
                this.hubStatus.syncAllotment(accountInfo)
            }

            var withdrawalStatus=contract.queryWithdrawalStatus(account)
            if (withdrawalStatus?.status == WithdrawalStatusType.INIT.number) {
                this.hubStatus.withdrawalStatus = withdrawalStatus
            }

            this.accountInfo()
            //this.dataStore.save(this.hubStatusData)
            watchHubEnvent()
            this.disconnect = false
        }

    }

    fun newTransfer(addr:Address, value:BigInteger) :OffchainTransaction{
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        val tx = OffchainTransaction(this.currentEon.id, account.address, addr, value)
        tx.sign(account.key)
        this.hubStatus.addOffchainTransaction(tx)

        val update = Update.newUpdate(
            this.currentEon.id,
            this.hubStatus.currentUpdate(currentEon).version + 1,
            this.account.address,
            this.hubStatus.currentTransactions()
        )
        update.sign(account.key)

        val iou = IOU(tx, update)

        val succResponse = hubServiceBlockingStub.sendNewTransfer(iou.toProto())
        //dataStore.save(this.hubStatusData)
        if (succResponse.getSucc() == true) {
            this.hubStatus.update = update
            return tx
        } else {
            throw RuntimeException("offlien transfer failed")
        }
    }

    fun deposit(value :BigInteger) {
        var chainTransaction=chain.newTransaction(account,contract.contractAddress, value)
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
        val stub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)
        try{
            hubAccount=HubAccount.parseFromProtoMessage(stub.getHubAccount(account.address.toProto()))
            return hubAccount
        }catch (e :StatusRuntimeException){
            if(e.status == Status.NOT_FOUND){
                LOG.warning("no such user")
                return null
            }
            throw e
        }
        return null
    }

    internal fun openTransferChallenge(transactionHash:Hash){
        val offchainTransaction=this.hubStatus.getTransactionByHash(transactionHash)
        val lastUpdate = this.hubStatus.lastUpdate(this.currentEon)
        val path = this.hubStatus.transactionPath(transactionHash)
        val transferDeliveryChallenge = TransferDeliveryChallenge(lastUpdate,offchainTransaction!!,path)
        this.contract.openTransferDeliveryChallenge(this.account,transferDeliveryChallenge)
        GlobalScope.launch { eonChannel?.send(ClientEventType.OPEN_TRANSFER_DELIVERY_CHALLENGE) }
    }

    internal fun onTransferDeliveryChallenge(transferDeliveryChallenge: TransferDeliveryChallenge){
        GlobalScope.launch { eonChannel?.send(ClientEventType.OPEN_TRANSFER_DELIVERY_CHALLENGE_PASS) }
    }

    fun withDrawal(value: BigInteger) {
        // 这里需要增加value是否大于本地余额的校验
        if (!hubStatus.couldWithDrawal()) {
            LOG.info("already have withdrawal in progress.")
            return
        }
        if (hubStatus.currentEonProof() == null) {
            LOG.info("last eon path doesn't exists.")
            return
        }

        val withdrawal = Withdrawal(hubStatus.lastEonProof()!!, value)
        this.contract.initiateWithdrawal(account,withdrawal)
    }

    internal fun cheat(flag:Int){
        val liquidityHubServiceBlockingStub = HubServiceGrpc.newBlockingStub(channelManager.hubChannel)

        val hubMaliciousFlag = Starcoin.HubMaliciousFlag.forNumber(flag)
        val builder = Starcoin.HubMaliciousFlags.newBuilder()
        builder.addFlags(hubMaliciousFlag)
        liquidityHubServiceBlockingStub.setMaliciousFlags(builder.build())
    }

    internal fun restore() {
    }

    private fun syncBlocks() {
    }

    private fun checkChallengeStatus() {
    }

    internal fun confirmDeposit(transaction: ChainTransaction){
        if(transaction.from?.equals(account.address)?:false)
            this.hubStatus.confirmDeposit(transaction)
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
            if(!Address.wrap(value.address).equals(account.address))
                return
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
                if(clientEvent!=ClientEventType.NOTHING){
                    eonChannel?.send(clientEvent)
                    LOG.info("send event $clientEvent to ${account.address}")
                }
            }
            //dataStore.save(this.hubStatusData)
        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
        }
    }

    internal fun onBalanceUpdateChallenge(proof:BalanceUpdateProof){
        LOG.info("open balance update challenge succ $proof")
        GlobalScope.launch {
            eonChannel?.send(ClientEventType.OPEN_BALANCE_UPDATE_CHALLENGE_PASS)
        }
    }
}
