package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.wallet.core.blockchain.BlockChainListener
import org.starcoin.sirius.wallet.core.store.FileStore
import org.starcoin.sirius.wallet.core.store.MemoryStore
import org.starcoin.sirius.wallet.core.store.Store

import java.security.KeyPair
import kotlin.properties.Delegates

class Wallet {

    private var hub: Hub by Delegates.notNull()
    private var address: Address by Delegates.notNull()

    private var blockChainListener :BlockChainListener by Delegates.notNull()

    internal var keyPair: KeyPair by Delegates.notNull()

    private var name: String by Delegates.notNull()

    constructor(name: String,contractAddress: Address,chainAddress:InetAddressPort,channelManager: ChannelManager, persistKey: Boolean) {
        var chain : Chain<EthereumTransaction, BlockInfo, HubContract>
        val store: Store<HubStatus>
        if (persistKey) {
            store = FileStore("", HubStatus::class.java)
            chain = EthereumChain(chainAddress.toHttpURL())
        } else {
            store = MemoryStore(HubStatus::class.java)
            chain = InMemoryChain(true)
        }

    }

    constructor(name: String, contractAddress: Address, channelManager: ChannelManager, keypair: KeyPair, store: Store<HubStatus>, chain:Chain<ChainTransaction, BlockInfo, HubContract>) {
        this.name = name
        this.keyPair = keypair
        this.address = Address.getAddress(keyPair.public)

        hub= Hub(contractAddress,address,channelManager,keyPair,null,store)

        this.blockChainListener = BlockChainListener(hub)

        chain.watchBlock {
            this.blockChainListener.onNewBlock(it)
        }
    }

    companion object {

        private val PRIVATE_KEY_FILENAME = "private"
        private val PUBLIC_KEY_FILENAME = "public"
        private val ADDRESS_FILENAME = "address"
    }

}
