package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.wallet.core.blockchain.BlockChainListener
import org.starcoin.sirius.wallet.core.store.Store
import kotlin.properties.Delegates

class Wallet<T:ChainTransaction>{

    private var hub: Hub by Delegates.notNull()
    private var address: Address by Delegates.notNull()

    private var blockChainListener :BlockChainListener by Delegates.notNull()

    private var keyPair: CryptoKey by Delegates.notNull()

    //TODO
    private var chain: Chain<T, *, *> by Delegates.notNull()

    constructor(contractAddress: Address, channelManager: ChannelManager,
                chain: Chain<T, *, *>, store: Store<HubStatus>, keypair: CryptoKey
    ) {
        hub= Hub(contractAddress,address,channelManager,keyPair,null,store)
        this.chain = chain

        this.blockChainListener = BlockChainListener(hub)

        chain.watchBlock {
            this.blockChainListener.onNewBlock(it)
        }
    }
}
