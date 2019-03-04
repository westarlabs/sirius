package org.starcoin.sirius.wallet.core

import kotlinx.coroutines.channels.Channel
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.blockchain.BlockChain
import java.math.BigInteger
import kotlin.properties.Delegates

class Wallet<T : ChainTransaction, A : ChainAccount> {

    internal var hub: Hub<T,A> by Delegates.notNull()
        private set

    private var blockChain: BlockChain<T,A> by Delegates.notNull()

    private var account: A by Delegates.notNull()

    //TODO
    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    constructor(contractAddress: Address, chain: Chain<T, out Block<T>, A>, account: A
    ) {
        this.chain = chain
        this.account = account

        val contract=chain.loadContract(contractAddress)

        hub = Hub(contract,account,null,chain)

        blockChain = BlockChain(chain,hub,contract,account)

        blockChain.startWatch=true
        blockChain.watchTransaction()

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

    fun restore() = hub.restore()
}
