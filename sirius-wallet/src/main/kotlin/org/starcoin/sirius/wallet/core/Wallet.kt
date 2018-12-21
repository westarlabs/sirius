package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.wallet.core.store.Store

import java.security.KeyPair
import kotlin.properties.Delegates

class Wallet {

    private var hub: Hub by Delegates.notNull()
    private var address: Address by Delegates.notNull()

    internal var keyPair: KeyPair by Delegates.notNull()

    var name: String

    constructor(name: String,chainAddress:InetAddressPort,channelManager: ChannelManager, persistKey: Boolean) {
        this.name = name
        //var chain = EthereumChain(chainAddress.toHttpURL())
    }

    constructor(
        name: String,channelManager: ChannelManager, keypair: KeyPair, store: Store<HubStatus>
    ) {
        this.name = name
        this.keyPair = keypair

       this.address = Address.getAddress(keyPair.public)

        var chain = InMemoryChain(true)
        chain.watchBlock {  }

    }

    companion object {

        private val PRIVATE_KEY_FILENAME = "private"
        private val PUBLIC_KEY_FILENAME = "public"
        private val ADDRESS_FILENAME = "address"
    }

}
