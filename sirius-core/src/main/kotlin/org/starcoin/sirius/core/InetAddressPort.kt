package org.starcoin.sirius.core

import org.starcoin.sirius.util.MockUtils
import java.net.InetSocketAddress

data class InetAddressPort(val host: String, val port: Int) {

    override fun toString(): String {
        return String.format("%s:%s", host, port)
    }

    fun toInetSocketAddress(): InetSocketAddress {
        return InetSocketAddress(host, port)
    }

    companion object {

        fun valueOf(hostAndPort: String): InetAddressPort {
            val parts = hostAndPort.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            require(parts.size == 2) { "invalid hostAndPort arg:$hostAndPort" }
            val port = Integer.parseInt(parts[1])
            return InetAddressPort(parts[0], port)
        }

        fun random(): InetAddressPort {
            return InetAddressPort(
                "127.0.0." + MockUtils.nextInt(1, 256), MockUtils.nextInt(1024, 10240)
            )
        }
    }
}
