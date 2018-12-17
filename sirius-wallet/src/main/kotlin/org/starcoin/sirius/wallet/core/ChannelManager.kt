package org.starcoin.sirius.wallet.core

import io.grpc.Channel
import io.grpc.netty.NettyChannelBuilder
import org.starcoin.sirius.core.InetAddressPort
import kotlin.properties.Delegates

class ChannelManager {

    var hubServer: InetAddressPort by Delegates.notNull()

    var contractServer: InetAddressPort by Delegates.notNull()

    var hubChannel: Channel? = null
        private set

    var contractChannel: Channel? = null
        private set

    constructor(hubServer: InetAddressPort, contractServer: InetAddressPort) {
        this.hubServer = hubServer
        this.contractServer = contractServer

        this.contractChannel =
                NettyChannelBuilder.forAddress(contractServer.toInetSocketAddress()).usePlaintext().build()
        this.hubChannel = NettyChannelBuilder.forAddress(hubServer.toInetSocketAddress()).usePlaintext().build()
    }

    constructor(hubChannel: Channel, contractChannel: Channel) {
        this.contractChannel = contractChannel
        this.hubChannel = hubChannel
    }
}
