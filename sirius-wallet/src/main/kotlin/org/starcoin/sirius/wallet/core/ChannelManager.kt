package org.starcoin.sirius.wallet.core

import io.grpc.Channel
import io.grpc.netty.NettyChannelBuilder
import org.starcoin.sirius.core.InetAddressPort
import kotlin.properties.Delegates

class ChannelManager {

    var hubServer: InetAddressPort by Delegates.notNull()

    var hubChannel: Channel? = null
        private set

    constructor(hubServer: InetAddressPort, contractServer: InetAddressPort) {
        this.hubServer = hubServer
        this.hubChannel = NettyChannelBuilder.forAddress(hubServer.toInetSocketAddress()).usePlaintext().build()
    }

    constructor(hubChannel: Channel) {
        this.hubChannel = hubChannel
    }
}

fun InetAddressPort.toHttpURL():String{
    return String.format("http://%s:%d",this.host,this.port)
}
