package org.starcoin.sirius.hub


import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import java.util.*


class Configuration private constructor(val properties: Properties) {

    enum class Network constructor(val magic: Int) {
        MAIN(0x468dccb0),
        TEST(0x35eff3f5),
        DEV(0x596612f),
        // for unit test.
        UINT(0xAB218E);
    }

    val rpcBind: InetAddressPort
        get() = InetAddressPort.valueOf(properties.getProperty("rpcBind", DEFAULT_RPC_BIND.toString()))

    val blocksPerEon: Int
        get() = Integer.valueOf(properties.getProperty("blocksPerEon", DEFAULT_BLOCKS_PER_EON.toString()))

    val ownerKey: CryptoKey
        get() = CryptoService.loadCryptoKey(
            properties.getProperty(
                "ownerKey",
                CryptoService.dummyCryptoKey.toBytes().toHEXString()
            ).hexToByteArray()
        )

    val isUnitNetwork: Boolean
        get() = this.network == Network.UINT

    val network: Network
        get() = Network.valueOf(properties.getProperty("network", Network.DEV.name))


    companion object {

        val DEFAULT_RPC_PORT = 7985
        val DEFAULT_RPC_BIND = InetAddressPort("0.0.0.0", DEFAULT_RPC_PORT)
        val DEFAULT_BLOCKS_PER_EON = 8

        fun loadConfiguration(): Configuration {
            var properties = Properties()
            Configuration::class.java.classLoader.getResource("hub.conf").openStream().use { properties.load(it) }
            return Configuration(properties)
        }

        fun configurationForUNIT(): Configuration {
            val properties = Properties()
            properties.put("network", Network.UINT.name)
            properties.put("rpcBind", InetAddressPort.random().toString())
            return Configuration(properties)
        }
    }
}
