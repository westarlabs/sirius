package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.*
import org.starcoin.sirius.wallet.core.store.Store
import java.security.KeyPair
import kotlin.properties.Delegates

class Hub {

    var contractAddress: BlockAddress  by Delegates.notNull()

    var walletAddress: BlockAddress  by Delegates.notNull()

    var currentEon: Eon  by Delegates.notNull();

    val blocksPerEon: Int = 0

    var hubObserver: HubObserver  by Delegates.notNull()

    var channelManager: ChannelManager by Delegates.notNull()

    var serverEventHandler: ServerEventHandler by Delegates.notNull()

    var keyPair: KeyPair by Delegates.notNull()

    var hubAddr: BlockAddress by Delegates.notNull()

    var hubAccount: HubAccount? = null

    var dataStore: Store<HubStatus> by Delegates.notNull()

    var hubStatus: HubStatus by Delegates.notNull()

    // for test lost connect
    var disconnect = true

    var alreadWatch = false

    constructor(
        contractAddress: BlockAddress,
        walletAddr: BlockAddress,
        channelManager: ChannelManager,
        keyPair: KeyPair,
        serverEventHandler: ServerEventHandler,
        eonStatusStore: Store<HubStatus>
    ) {
        this.contractAddress = contractAddress
        this.walletAddress = walletAddr
        this.channelManager = channelManager
        this.serverEventHandler = serverEventHandler
        this.keyPair = keyPair
        this.dataStore = eonStatusStore

        //val protoHubInfo = hubServiceBlockingStub.getHubInfo(Empty.newBuilder().build())
        //val hubInfo = HubInfo(protoHubInfo)
        //this.hubAddr = BlockAddress.genBlockAddressFromPublicKey(hubInfo.getPublicKey())

        //this.blocksPerEon = hubInfo.getBlocksPerEon()

        //this.currentEon = this.getChainEon()

        //this.hubStatusData = HubStatusData(this.currentEon)
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
    fun checkNewBlock(block: BlockInfo) {
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

    fun newTransfer( addr:BlockAddress,  value:Int,  keyPair:KeyPair) :OffchainTransaction?{
        return null
    }

    fun deposit(value :Int,  keyPair:KeyPair) {
    }

    fun register( keyPair:KeyPair) : Update? {
        return null
    }

    fun  accountInfo():HubAccount ?{
        return null
    }

    fun openTransferChallenge( transactionHash:String,  keyPair:KeyPair) :Boolean?{
        return true
    }

    fun newTransferChallenge (
     update:Update,  path:MerkleTree<OffchainTransaction>,  transaction:OffchainTransaction,  keyPair:KeyPair): ChainTransaction?{
        return null
    }

    fun withDrawal(value: Int,  keyPair:KeyPair) {

    }

    fun cheat(flag:Int){

    }

    fun restore() {
    }

    private fun syncBlocks() {
    }

    private fun checkChallengeStatus() {
    }


}