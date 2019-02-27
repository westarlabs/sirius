package org.starcoin.sirius.wallet.core

import kotlinx.coroutines.channels.Channel
import org.sql2o.Sql2o
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.blockchain.BlockChain
import org.starcoin.sirius.wallet.core.dao.SiriusObjectDao
import org.starcoin.sirius.wallet.core.store.H2DatabaseStore
import org.starcoin.sirius.wallet.core.store.Store
import java.math.BigInteger
import kotlin.properties.Delegates

class Wallet<T : ChainTransaction, A : ChainAccount> {

    internal var hub: Hub<T,A> by Delegates.notNull()
        private set

    private var blockChain: BlockChain<T,A> by Delegates.notNull()

    private var account: A by Delegates.notNull()

    //TODO
    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    private val h2databaseUrl = "jdbc:h2:%s/data:starcoin;FILE_LOCK=FS;PAGE_SIZE=1024;CACHE_SIZE=819"
    private val h2databaseUrlMemory = "jdbc:h2:mem:starcoin"

    constructor(contractAddress: Address, channelManager: ChannelManager,
                chain: Chain<T, out Block<T>, A>, account: A ,homeDir:String?
    ) {
        this.chain = chain
        this.account = account

        val contract=chain.loadContract(contractAddress)

        hub = Hub(contract,account,channelManager,null,chain)

        initDB(homeDir,hub)
        blockChain = BlockChain(chain,hub,contract,account)

        blockChain.startWatch=true
        blockChain.watchTransaction()

    }

    private fun initDB(homeDir: String?,hub:Hub<T,A>){
        var url :String? = null
        if(homeDir!=null)
            url=h2databaseUrl.format(homeDir)
        else
            url=h2databaseUrlMemory
        val sql2o = Sql2o(url,"","")
        val updateH2Ds = H2DatabaseStore(sql2o,"update")
        val offchainTransactionH2Ds = H2DatabaseStore(sql2o,"offchain_transaction")
        val proofH2Ds = H2DatabaseStore(sql2o,"proof")

        hub.updateDao = SiriusObjectDao(updateH2Ds,{Update.parseFromProtobuf(it)})
        hub.offchainTransactionDao = SiriusObjectDao(offchainTransactionH2Ds,{OffchainTransaction.parseFromProtobuf(it)})
        hub.aMTreeProofDao = SiriusObjectDao(proofH2Ds,{AMTreeProof.parseFromProtobuf(it)})

        hub.updateDao.init()
        hub.offchainTransactionDao.init()
        hub.aMTreeProofDao.init()
    }

    fun deposit(value:BigInteger) = hub.deposit(value)

    fun balance():BigInteger = hub.getAvailableCoin()

    fun withdrawal(value:BigInteger)= hub.withDrawal(value)

    fun register():Update?= hub.register()

    fun openTransferChallenge(hash:Hash)= this.hub.openTransferChallenge(hash)

    internal fun initMessageChannel(){
        hub.eonChannel = Channel(200)
    }

    internal fun getMessageChannel():Channel<ClientEventType>?{
        return hub.eonChannel
    }

    fun hubTransfer(to:Address,value:BigInteger)=hub.newTransfer(to,value)

    internal fun hubAccount():HubAccount?=hub.accountInfo()

    internal fun cheat(flag:Int)= hub.cheat(flag)

    fun sync() = hub.sync()
}
