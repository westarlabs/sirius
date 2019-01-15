package org.starcoin.sirius.wallet.core

import io.grpc.StatusRuntimeException
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.chain.ChainStrategy
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.wallet.core.store.Store
import java.math.BigInteger
import java.security.KeyPair
import kotlin.properties.Delegates

class Hub <T : ChainTransaction, A : ChainAccount> {

    private var contract: HubContract<A>  by Delegates.notNull()

    private var account: A  by Delegates.notNull()

    private var currentEon: Eon  by Delegates.notNull();

    private var channelManager: ChannelManager by Delegates.notNull()

    private var serverEventHandler: ServerEventHandler?

    private var hubAddr: String by Delegates.notNull()

    private var hubAccount: HubAccount? = null

    private var dataStore: Store<HubStatus> by Delegates.notNull()

    private var hubStatus: HubStatus by Delegates.notNull()

    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    // for test lost connect
    var disconnect = true

    var alreadWatch = false

    constructor(
        contract: HubContract<A>,
        account: A,
        channelManager: ChannelManager,
        serverEventHandler: ServerEventHandler?,
        eonStatusStore: Store<HubStatus>,
        chain :Chain<T, out Block<T>, A>,
        hubStatus: HubStatus
    ) {
        this.contract = contract
        this.account = account
        this.channelManager = channelManager
        this.serverEventHandler = serverEventHandler
        this.dataStore = eonStatusStore
        this.chain = chain
        this.hubStatus = hubStatus

        this.currentEon = this.getChainEon()

    }

    private fun getChainEon():Eon{
        var hubInfo=contract.queryHubInfo(account)
        hubAddr=hubInfo.hubAddress
        hubStatus.blocksPerEon= hubInfo.blocksPerEon
        return Eon.calculateEon(blocksPerEon = hubInfo.blocksPerEon,blockHeight = chain.getBlockNumber().toLong())
    }

    private fun onHubRootCommit(hubRoot: HubRoot) {

    }

    private fun onDeposit(deposit: Deposit) {
    }

    private fun onWithdrawal(withdrawalStatus: WithdrawalStatus) {
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

    private fun openBalanceUpdateChallenge(lastUpdate:Update ,  lastIndex:Int) {
    }

    private fun depositTransaction( chainTransaction:ChainTransaction)
    {
    }

    fun getAvailableCoin():Long {
        return 0;
    }

    fun getWithdrawalCoin():Long {
        return 0;
    }

    fun couldWithDrawal():Boolean {
        return false
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
            this.disconnect = false
            return response
        } catch (e: StatusRuntimeException) {
            throw e
        }

    }

    fun  accountInfo():HubAccount ?{
        return null
    }

    fun openTransferChallenge( transactionHash:String) :Boolean?{
        return true
    }

    fun newTransferChallenge (
        update: Update, path: MerkleTree, transaction: OffchainTransaction
    ): ChainTransaction? {
        return null
    }

    fun withDrawal(value: Int) {

    }

    fun cheat(flag:Int){

    }

    fun restore() {
    }

    private fun syncBlocks() {
    }

    private fun checkChallengeStatus() {
    }

    fun confirmDeposit(hash:Hash,height :Int){
        this.hubStatus.confirmDeposit(hash)
        hubStatus.height=height
        //dataStore.save(this.hubStatus)
    }
}
