package org.starcoin.sirius.wallet.core

import com.google.protobuf.Empty
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.TxDeferred
import org.starcoin.sirius.serialization.rlp.toByteArray
import org.starcoin.sirius.util.WithLogging
import java.lang.IllegalStateException
import java.math.BigInteger
import kotlin.properties.Delegates

class Hub <T : ChainTransaction, A : ChainAccount> {

    companion object : WithLogging()

    private var contract: HubContract<A>  by Delegates.notNull()

    private var account: ClientAccount<A>  by Delegates.notNull()

    internal var currentEon: Eon  by Delegates.notNull()
    private set

    private var serverEventHandler: ServerEventHandler?

    private var hubAddr: String by Delegates.notNull()

    private var hubAccount: HubAccount? = null

    internal var hubStatus: HubStatus<A> by Delegates.notNull()
        private set

    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    internal var eonChannel :Channel<ClientEventType>? = null

    // for test lost connect
    internal var disconnect = false

    var alreadWatch = false

    private var currentEonKey = "current-eon".toByteArray()

    internal var hubInfo:ContractHubInfo

    internal var checkBalance = true

    constructor(
        contract: HubContract<A>,
        account: ClientAccount<A>,
        serverEventHandler: ServerEventHandler?,
        chain :Chain<T, out Block<T>, A>
    ) {
        this.contract = contract
        this.account = account
        this.serverEventHandler = serverEventHandler
        this.chain = chain

        hubInfo=contract.queryHubInfo(account.account)
        hubAddr=hubInfo.hubAddress

        this.currentEon=Eon.calculateEon(startBlockNumber = hubInfo.startBlockNumber.toLong(),blocksPerEon = hubInfo.blocksPerEon,currentBlockNumber = chain.getBlockNumber().toLong())

        this.hubStatus = HubStatus(this.currentEon,account)
        hubStatus.blocksPerEon= hubInfo.blocksPerEon

        LOG.info(getHubInfo().toJSON())

        this.hubAccount=this.accountInfo()
    }

    internal fun hasRegister():Boolean{
        return this.hubAccount!=null
    }

    private fun getChainEon():Eon{
        var hubInfo=contract.queryHubInfo(account.account)
        return Eon.calculateEon(startBlockNumber = hubInfo.startBlockNumber.toLong(),blocksPerEon = hubInfo.blocksPerEon,currentBlockNumber = chain.getBlockNumber().toLong())
    }

    private fun getHubInfo():HubInfo{// this hubinfo from hub ,not hubinfo of contract
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)
        val hubInfo:HubInfo=hubServiceBlockingStub.getHubInfo(Empty.getDefaultInstance()).toSiriusObject()
        return hubInfo
    }

    @Synchronized
    internal fun onHubRootCommit(hubRoot: HubRoot) {
        try {
            // System.out.println("get new hub root"+hubRoot.toString());
            LOG.info("start get eon")
            val eon = this.getChainEon()
            LOG.info("finish get eon")

            if (this.accountInfo() == null||this.disconnect) {
                GlobalScope.launch { eonChannel?.send(ClientEventType.FINISH_EON_CHANGE) }
                return
            }
            LOG.info("current eon is "+eon.id+" hubroot eon is " +hubRoot.eon)
            var verifyResult :Boolean
            if (eon.id == hubRoot.eon) {
                LOG.info("start change eon")
                verifyResult=nextEon(hubRoot)
                LOG.info("finish change eon")
            } else {
                GlobalScope.launch { eonChannel?.send(ClientEventType.EON_CHANGE_EXCEPTION) }
                LOG.warning("hub eon is not right")
                verifyResult= false
            }
            serverEventHandler?.onHubRootCommit(hubRoot,verifyResult)
        } catch (e: Exception) {
            LOG.warning(e.message)
            serverEventHandler?.onHubRootCommit(hubRoot,false)
        }
    }

    internal fun cancelWithdrawal(cancelWithdrawal: CancelWithdrawal){
        this.hubStatus.cancelWithDrawal()
        GlobalScope.launch {
            eonChannel?.send(ClientEventType.CANCEL_WITHDRAWAL)
        }

        LOG.info("cancel withdrawal $cancelWithdrawal")
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
        if(this.hubStatus.hasProcessed(offchainTransaction)){
            LOG.info("transaction $offchainTransaction has been processed")
        }

        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

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

    internal fun recieveTransacion() {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)
        val offchainTransactionList=hubServiceBlockingStub.queryNewTransfer(this.account.address.toProto())
        if(offchainTransactionList!=null)
            offchainTransactionList.txsList.forEach{
                this.onNewTransaction(it.toSiriusObject())
            }
    }

    internal fun recieveHubSign() {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)
        val update=hubServiceBlockingStub.queryUpdate(this.account.address.toProto())
        this.onNewUpdate(update.toSiriusObject())
    }

    internal fun hubInfo():ContractHubInfo{
        return contract.queryHubInfo(account.account)
    }

    private fun onNewUpdate(update: Update) {
        LOG.info("get hub sign")
        if(this.hubStatus.hasProcessed(update)){
            LOG.info("update $update has been processed")
            return
        }

        if(!(this.hubStatus.update?.sign?.equals(update.sign)?:false)){
            LOG.warning("local update is ${this.hubStatus.update}, hub update is ${update}")
            LOG.warning("sign of hub update is not right")
            return
        }
        this.hubStatus.addUpdate(update)
        serverEventHandler?.onNewUpdate(update)
    }

    private fun nextEon(hubRoot: HubRoot) :Boolean{
        this.currentEon = this.getChainEon()

        this.checkChallengeStatus()
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

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

        LOG.info("proof hub allot is "+ selfNode.allotment)
        LOG.info("local allot is " + this.getAvailableCoin())

        if (selfNode.allotment < this.getAvailableCoin()) {
            needChallenge = true
        }else {
            LOG.info("challenge is not necessary")
        }

        GlobalScope.launch {
            eonChannel?.send(ClientEventType.FINISH_EON_CHANGE)
        }

        ResourceManager.instance(account.name).dataStore.put(this.currentEonKey,this.currentEon.id.toByteArray())

        if (!needChallenge) {
            return !needChallenge
        }
        var balanceUpdateProof=this.hubStatus.newChallenge(lastUpdate)
        this.contract.openBalanceUpdateChallenge(account.account,balanceUpdateProof)

        GlobalScope.launch {
            eonChannel?.send(ClientEventType.OPEN_BALANCE_UPDATE_CHALLENGE)
        }
        return !needChallenge
    }

    internal fun getAvailableCoin():BigInteger {
        return hubStatus.getAvailableCoin(this.currentEon)
    }

    internal fun getWithdrawalCoin():BigInteger {
        return hubStatus.withdrawalStatus?.withdrawalAmount?: BigInteger.ZERO
    }

    @Synchronized
    internal fun sync() {
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }

        ResourceManager.instance(account.name).cleanData()
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

        this.currentEon = this.getChainEon()

        this.hubStatus = HubStatus(this.currentEon,account)

        for (i in 0..2) {
            val eonId = this.currentEon.id - i
            if (eonId < 0) {
                break
            }
            val index = this.hubStatus.getEonByIndex(-i)

            val accountInfo = hubServiceBlockingStub.getHubAccount(account.address.toProto())
            this.hubStatus.eonStatuses[index].updateHistory.add(accountInfo.eonState.update.toSiriusObject())
            this.hubStatus.eonStatuses[index].transactionHistory.addAll(
                accountInfo.eonState.txsList.map { it.toSiriusObject<Starcoin.OffchainTransaction, OffchainTransaction>() }
            )

            if (i > 0) { // 当前伦次不需要proof

                if (eonId > 0) {
                    val blockAddressAndEonBuilder = Starcoin.BlockAddressAndEon.newBuilder()
                    blockAddressAndEonBuilder.address = account.address.toByteString()
                    blockAddressAndEonBuilder.eon = eonId

                    try {
                        val proof = hubServiceBlockingStub.getProofWithEon(blockAddressAndEonBuilder.build())
                        this.hubStatus.eonStatuses[index].treeProof = proof.toSiriusObject()
                    }catch (e:StatusRuntimeException){
                        LOG.info("proof of eon $eonId not exists")
                    }

                }
            } else { // 当前轮次同步余额
                this.hubStatus.syncAllotment(accountInfo)
            }

            try{
                var withdrawalStatus=contract.queryWithdrawalStatus(account.account)
                if (withdrawalStatus?.status == WithdrawalStatusType.INIT.number) {
                    this.hubStatus.withdrawalStatus = withdrawalStatus
                }
            }catch (e:Exception){
                LOG.warning(e.message)
            }

            this.accountInfo()
            watchHubEnvent()

            this.disconnect = false
        }

    }

    internal fun newTransfer(addr:Address, value:BigInteger) :OffchainTransaction{
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }
        if(value>this.getAvailableCoin()&&checkBalance){
            val message = "transfer $value  is bigger than balance ${this.getAvailableCoin()}"
            LOG.info(message)
            throw IllegalStateException(message)
        }

        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

        val tx = OffchainTransaction(this.currentEon.id, account.address, addr, value)
        tx.sign(account.key)

        val update = Update.newUpdate(
            this.currentEon.id,
            this.hubStatus.currentUpdate(currentEon).version + 1,
            this.account.address,
            mutableListOf<OffchainTransaction>().apply {
                addAll(hubStatus.currentTransactions())
                add(tx)}
        )
        update.sign(account.key)

        val iou = IOU(tx, update)

        val succResponse = hubServiceBlockingStub.sendNewTransfer(iou.toProto())
        if (succResponse.getSucc() == true) {
            this.hubStatus.update = update
            this.hubStatus.addOffchainTransaction(tx)
            return tx
        } else {
            fail { "offlien transfer failed" }
        }
    }

    internal fun deposit(value :BigInteger) :ChainTransaction{
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }
        val chainTransaction=chain.newTransaction(account.account,contract.contractAddress, value)
        var txDeferred = chain.submitTransaction(account.account, chainTransaction)
        this.hubStatus.addDepositTransaction(txDeferred.txHash, chainTransaction)
        return chainTransaction
    }

    internal fun chainTransaction(addr: Address,value :BigInteger):T {
        val chainTransaction=chain.newTransaction(account.account,addr, value)
        val txDeferred = chain.submitTransaction(account.account, chainTransaction)
        LOG.info("tx deferred is $txDeferred ")
        return chainTransaction
    }

    internal fun register() : Update? {
        val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

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

    internal fun  accountInfo():HubAccount ?{
        val stub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)
        try{
            hubAccount=HubAccount.parseFromProtoMessage(stub.getHubAccount(account.address.toProto()))
            return hubAccount
        }catch (e :StatusRuntimeException){
            if(e.status == Status.NOT_FOUND){
                LOG.info("no such user")
                return null
            }
            throw e
        }
    }

    internal fun openTransferChallenge(transactionHash:Hash){
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }
        val offchainTransaction=this.hubStatus.getTransactionByHash(transactionHash)
        val lastUpdate = this.hubStatus.lastUpdate()
        val path = this.hubStatus.transactionPath(transactionHash)
        val transferDeliveryChallenge = TransferDeliveryChallenge(lastUpdate,offchainTransaction!!,path)
        this.contract.openTransferDeliveryChallenge(this.account.account,transferDeliveryChallenge)
        GlobalScope.launch { eonChannel?.send(ClientEventType.OPEN_TRANSFER_DELIVERY_CHALLENGE) }
    }

    internal fun onTransferDeliveryChallenge(transferDeliveryChallenge: TransferDeliveryChallenge){
        LOG.info(transferDeliveryChallenge.toJSON())
        GlobalScope.launch { eonChannel?.send(ClientEventType.OPEN_TRANSFER_DELIVERY_CHALLENGE_PASS) }
    }

    internal fun withDrawal(value: BigInteger):TxDeferred {
        // 这里需要增加value是否大于本地余额的校验
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }
        if (!hubStatus.couldWithDrawal()) {
            LOG.info("already have withdrawal in progress.")
            throw IllegalStateException("already have withdrawal in progress.")
        }
        if (hubStatus.lastEonProof() == null) {
            LOG.info("last eon path doesn't exists.")
            throw IllegalStateException("last eon path doesn't exists.")
        }
        if(value>this.getAvailableCoin()&&checkBalance){
            val message = "withdrawal $value  is bigger than balance ${this.getAvailableCoin()}"
            LOG.info(message)
            throw IllegalStateException(message)
        }

        val withdrawal = Withdrawal(hubStatus.lastEonProof()!!, value)
        return this.contract.initiateWithdrawal(account.account,withdrawal)
    }

    internal fun cheat(flag:Int){
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }

        val liquidityHubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)

        val hubMaliciousFlag = Starcoin.HubMaliciousFlag.forNumber(flag)
        val builder = Starcoin.HubMaliciousFlags.newBuilder()
        builder.addFlags(hubMaliciousFlag)
        liquidityHubServiceBlockingStub.setMaliciousFlags(builder.build())
    }

    private fun checkChallengeStatus() {
    }

    internal fun confirmDeposit(transaction: ChainTransaction,deposit: Deposit){
        if(transaction.from?.equals(account.address)?:false)
            this.hubStatus.confirmDeposit(transaction)
        serverEventHandler?.onDeposit(deposit)
    }

    @Synchronized
    private fun watchHubEnvent() {
        if (alreadWatch == true) {
            return
        }

        val hubServiceStub = HubServiceGrpc.newStub(ResourceManager.hubChannel)

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
                    LOG.warning("send event $clientEvent to ${account.address}")
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

    internal fun restore(){
        if(this.hubAccount==null){
            throw IllegalStateException("need reg/login first")
        }

        val lastSavedEon=ResourceManager.instance(account.name).dataStore.get(this.currentEonKey)?.toBigInteger()?.toInt()?:0
        val currentEon=this.getChainEon()
        if((currentEon.id-lastSavedEon)>1){
            throw java.lang.IllegalStateException("local data is too old,please use sync command")
        }
        this.hubStatus.reloadData(lastSavedEon)
        if((currentEon.id>lastSavedEon)){
            val hubServiceBlockingStub = HubServiceGrpc.newBlockingStub(ResourceManager.hubChannel)
            val hubInfo:HubInfo=hubServiceBlockingStub.getHubInfo(Empty.getDefaultInstance()).toSiriusObject()
            onHubRootCommit(HubRoot(hubInfo.root,hubInfo.eon))
        }
    }
}
