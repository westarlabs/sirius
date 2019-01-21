package org.starcoin.sirius.wallet.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.blockchain.BlockChain
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

    constructor(contractAddress: Address, channelManager: ChannelManager,
                chain: Chain<T, out Block<T>, A>, store: Store<HubStatus>?, account: A
    ) {
        this.chain = chain
        this.account = account

        val contract=chain.loadContract(contractAddress)
        hub = Hub(contract,account,channelManager,null,store,chain)
        blockChain = BlockChain(chain,hub,contract,account)

        blockChain.startWatch=true
        blockChain.watchTransaction()

    }

    fun deposit(value:Long){
        this.hub.deposit(value)
    }

    fun balance():BigInteger{
        return hub.getBalance()
    }

    fun withdrawal(value:Long){
        return hub.withDrawal(value)
    }

    fun register():Update?{
        return hub.register()
    }

    internal fun initMessageChannel(){
        hub.eonChannel = Channel(200)
    }

    internal fun getMessageChannel():Channel<Eon>?{
        return hub.eonChannel
    }
}
