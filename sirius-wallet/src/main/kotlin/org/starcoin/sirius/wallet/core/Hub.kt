package org.starcoin.sirius.wallet.core

import io.grpc.StatusRuntimeException
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.wallet.core.store.Store
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

    private fun onHubRootCommit(hubRoot: HubRoot) {

    }

    internal fun onDeposit(deposit: Deposit) {
        serverEventHandler?.onDeposit(deposit)
    }

    internal fun onWithdrawal(withdrawalStatus: WithdrawalStatus) {
        if (withdrawalStatus.withdrawal.address.equals(account.address)) {
            when (withdrawalStatus.status) {
                WithdrawalStatusType.INIT.number -> {
                    this.hubStatus.syncWithDrawal(withdrawalStatus)
                }
                WithdrawalStatusType.CANCEL.number -> {
                    this.hubStatus.cancelWithDrawal()
                    LOG.info("cancel")
                }
                WithdrawalStatusType.PASSED.number -> {
                    this.hubStatus.syncWithDrawal(withdrawalStatus)
                    LOG.info("pass")
                }
                else -> System.out.println(withdrawalStatus)
            }
        }
        serverEventHandler?.onWithdrawal(withdrawalStatus)
    }

    private fun onNewTransaction(offchainTransaction: OffchainTransaction) {
    }

    fun recieveTransacion() {

    }

    fun recieveHubSign() {
    }

    private fun onNewUpdate(update: Update) {
    }

    @Synchronized
    private fun watchHubEnvent() {
    }

    @Synchronized
    private fun nextEon() {
    }

    internal fun openBalanceUpdateChallenge(){
        val balanceUpdateProof = BalanceUpdateProof(this.hubStatus.currentUpdate(this.currentEon),this.hubStatus.currentEonProof())
        var challenge=BalanceUpdateChallenge(balanceUpdateProof,account.key.keyPair.public)
        this.contract.openBalanceUpdateChallenge(account,challenge)
    }

    fun getAvailableCoin():Long {
        return 0;
    }

    fun getWithdrawalCoin():Long {
        return 0;
    }

    @Synchronized
    fun sync() {
    }

    fun newTransfer(addr:Address, value:Int) :OffchainTransaction?{
        return null
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
            //this.disconnect = false
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

        val withdrawal = Withdrawal(account.address, hubStatus.currentEonProof()!!, value)
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

}
