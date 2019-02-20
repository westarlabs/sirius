package org.starcoin.sirius.hub


import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.ethereum.InMemoryChainProvider
import java.io.File
import java.net.URI
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

    val ownerKeystore: File?
        get() = properties.getProperty("ownerKeystore")?.let { File(it) }

    val ownerKeystorePassword: String
        get() = properties.getProperty("ownerKeystorePassword", "")

    val isUnitNetwork: Boolean
        get() = this.network == Network.UINT

    val network: Network
        get() = Network.valueOf(properties.getProperty("network", Network.DEV.name))

    val connector: URI
        get() = URI(properties.getProperty("connector", DEFAULT_CONNECTOR))

    val autoDeployContract: Boolean
        get() = properties.getProperty("autoDeployContract", "true").equals("true", true)

    val contractAddress: Address?
        get() = properties.getProperty("contractAddress")?.let { Address.wrap(it) }

    companion object {

        val DEFAULT_RPC_PORT = 7985
        val DEFAULT_RPC_BIND = InetAddressPort("0.0.0.0", DEFAULT_RPC_PORT)
        val DEFAULT_BLOCKS_PER_EON = 8
        val DEFAULT_CONNECTOR = "${InMemoryChainProvider.scheme}:test"

        fun loadConfiguration(): Configuration {
            var properties = Properties()
            Configuration::class.java.classLoader.getResource("hub.conf").openStream().use { properties.load(it) }
            return Configuration(properties)
        }

        fun configurationForUNIT(): Configuration {
            val properties = Properties()
            properties.put("network", Network.UINT.name)
            properties.put("rpcBind", InetAddressPort.random().toString())
            properties.put("connector", DEFAULT_CONNECTOR)
            properties.put("autoDeployContract", "true")
            return Configuration(properties)
        }
    }
}
