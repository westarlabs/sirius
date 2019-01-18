package org.starcoin.sirius.hub


import com.google.common.base.Preconditions
import org.starcoin.sirius.core.InetAddressPort
import java.util.*
import kotlin.properties.Delegates

class Configuration private constructor(val network: Network) {

    var rpcBind: InetAddressPort by Delegates.notNull()
        private set
    //TODO
    var blocksPerEon = 8
        private set


    val isUnitNetwork: Boolean
        get() = this.network == Network.UINT

    enum class Network constructor(val magic: Int) {
        MAIN(0x468dccb0),
        TEST(0x35eff3f5),
        DEV(0x596612f),
        // for unit test.
        UINT(0xAB218E);
    }

    class ConfigurationBuilder private constructor(private val configuration: Configuration) {

        fun setRpcBind(rpcBind: InetAddressPort): ConfigurationBuilder {
            Preconditions.checkNotNull(rpcBind, "rpcBind")
            this.configuration.rpcBind = rpcBind
            return this
        }

        fun setBlocksPerEon(blocks: Int): ConfigurationBuilder {
            Preconditions.checkArgument(blocks > 0, "blocks per eon should bigger than zero.")
            this.configuration.blocksPerEon = blocks
            return this
        }

        fun build(): Configuration {
            // TODO check properties.
            return configuration
        }

        companion object {

            fun forNetwork(network: Network): ConfigurationBuilder {
                return ConfigurationBuilder(Configuration(network))
            }
        }
    }

    companion object {

        val DEFAULT_RPC_PORT = 7985
        val DEFAULT_RPC_BIND = InetAddressPort("0.0.0.0", DEFAULT_RPC_PORT)
        val DEFAULT_BLOCKS_PER_EON = 8

        fun loadConfiguration(): Configuration {
            var properties = Properties()
            Configuration::class.java.classLoader.getResource("hub.conf").openStream().use { properties.load(it) }


            val networkName = properties.getProperty("network", Network.DEV.name)

            val network = Network.valueOf(networkName)

            val builder = ConfigurationBuilder.forNetwork(network)


            val rpcHostPort = properties.getProperty("rpcBind", DEFAULT_RPC_BIND.toString())
            val rpcBind = InetAddressPort.valueOf(rpcHostPort)
            builder.setRpcBind(rpcBind)

            val blocksPerEon =
                Integer.valueOf(properties.getProperty("blocksPerEon", DEFAULT_BLOCKS_PER_EON.toString()))
            builder.setBlocksPerEon(blocksPerEon)

            return builder.build()
        }

        fun configurationForUNIT(): Configuration {
            val configuration = Configuration(Network.UINT)
            configuration.rpcBind = InetAddressPort.random()
            return configuration
        }
    }
}
