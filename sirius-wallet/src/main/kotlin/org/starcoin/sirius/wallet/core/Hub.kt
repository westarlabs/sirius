package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Eon
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.wallet.core.store.Store
import java.security.KeyPair
import kotlin.properties.Delegates

class Hub {

     val contractAddress: BlockAddress  by Delegates.notNull()

     val walletAddress: BlockAddress  by Delegates.notNull()

     var currentEon: Eon  by Delegates.notNull();

     val blocksPerEon: Int = 0

     var hubObserver: HubObserver  by Delegates.notNull()

     val channelManager: ChannelManager by Delegates.notNull()

     val serverEventHandler: ServerEventHandler by Delegates.notNull()

     val keyPair: KeyPair by Delegates.notNull()

     val hubAddr: BlockAddress by Delegates.notNull()

     var hubAccount: HubAccount? = null

     var dataStore: Store<HubStatus> by Delegates.notNull()

     var hubStatus: HubStatus by Delegates.notNull()

    // for test lost connect
     var disconnect = true

     var alreadWatch = false


}