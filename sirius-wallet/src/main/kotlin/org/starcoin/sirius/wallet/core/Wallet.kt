package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.wallet.core.store.Store

import java.security.KeyPair
import kotlin.properties.Delegates

class Wallet {

    private var hubStatus: HubStatus by Delegates.notNull()
    private var address: BlockAddress by Delegates.notNull()

    internal var keyPair: KeyPair by Delegates.notNull()

    var name: String


    constructor(name: String, channelManager: ChannelManager, persistKey: Boolean) {
        this.name = name
    }

    constructor(
        name: String, channelManager: ChannelManager, keypair: KeyPair, store: Store<HubStatus>
    ) {
        this.name = name
        this.keyPair = keypair
        this.address = BlockAddress.genBlockAddressFromPublicKey(keyPair.public)
    }

    companion object {

        private val PRIVATE_KEY_FILENAME = "private"
        private val PUBLIC_KEY_FILENAME = "public"
        private val ADDRESS_FILENAME = "address"
    }


}
