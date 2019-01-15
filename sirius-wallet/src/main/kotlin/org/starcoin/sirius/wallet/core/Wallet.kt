package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.blockchain.BlockChainListener
import org.starcoin.sirius.wallet.core.store.Store
import kotlin.properties.Delegates

class Wallet<T : ChainTransaction, A : ChainAccount> {

    private var hub: Hub<T,A> by Delegates.notNull()

    private var blockChainListener :BlockChainListener<T,A> by Delegates.notNull()

    private var account: A by Delegates.notNull()

    //TODO
    private var chain: Chain<T, out Block<T>, A> by Delegates.notNull()

    constructor(contractAddress: Address, channelManager: ChannelManager,
                chain: Chain<T, out Block<T>, A>, store: Store<HubStatus>, account: A
    ) {
        this.chain = chain
        this.account = account

        var contract=chain.loadContract(contractAddress)
        hub = Hub(contract,account,channelManager,null,store,chain)
        this.blockChainListener = BlockChainListener(hub)
    }
}
