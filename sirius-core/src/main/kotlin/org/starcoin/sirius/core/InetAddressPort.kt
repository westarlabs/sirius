package org.starcoin.sirius.core

import com.google.common.base.Preconditions
import org.starcoin.sirius.util.MockUtils
import java.net.InetSocketAddress
import java.util.*

class InetAddressPort(val host: String, val port: Int) {

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is InetAddressPort) {
            return false
        }
        val that = o as InetAddressPort?
        return port == that!!.port && host == that.host
    }

    override fun hashCode(): Int {

        return Objects.hash(host, port)
    }

    override fun toString(): String {
        return String.format("%s:%s", host, port)
    }

    fun toInetSocketAddress(): InetSocketAddress {
        return InetSocketAddress(host, port)
    }

    companion object {

        fun valueOf(hostAndPort: String): InetAddressPort {
            val parts = hostAndPort.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Preconditions.checkState(parts.size == 2, "invalid hostAndPort arg:$hostAndPort")
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
