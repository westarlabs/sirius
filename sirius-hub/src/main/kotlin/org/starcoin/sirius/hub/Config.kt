package org.starcoin.sirius.hub


import com.google.common.base.Preconditions
import com.google.common.io.Files
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.delegate
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.ethereum.InMemoryChainProvider
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.net.URI
import java.util.*


class Config private constructor(val properties: Properties, val dataDir: File, val configFile: File) {

    enum class Network constructor(val magic: Int) {
        MAIN(0x468dccb0),
        TEST(0x35eff3f5),
        DEV(0x596612f),
        // for unit test.
        UINT(0xAB218E);
    }

    val rpcBind: InetAddressPort by properties.delegate { InetAddressPort.valueOf(this) }.default(DEFAULT_RPC_BIND)

    val blocksPerEon: Int by properties.delegate { Integer.valueOf(this) }.default(DEFAULT_BLOCKS_PER_EON)

    /**
     * owner private key, please use keystore for security reason
     */
    val ownerKey: CryptoKey by properties.delegate { CryptoService.loadCryptoKey(this.hexToByteArray()) }.default(
        CryptoService.dummyCryptoKey
    )

    /**
     * keystore dir
     */
    val ownerKeystore: File? by properties.delegate { File(this) }

    /**
     * keystore password
     */
    val ownerKeystorePassword: String by properties.delegate().default("")

    /**
     * account id or address for find keystore file in keystore dir
     */
    val accountIDOrAddress: String by properties.delegate().default("0")

    val isUnitNetwork: Boolean
        get() = this.network == Network.UINT

    val network: Network by properties.delegate { Network.valueOf(this) }.default(Network.DEV)

    val connector: URI by properties.delegate { URI(this) }.default(DEFAULT_CONNECTOR)

    /**
     * if contractAddress is not set, hub will auto deploy contracct.
     */
    var contractAddress: Address? by properties.delegate { Address.wrap(this) }

    fun store() {
        LOG.info("Store config to ${configFile.absolutePath}")
        configFile.outputStream().use {
            properties.store(it, "")
        }
    }

    companion object : WithLogging() {

        val DEFAULT_RPC_PORT = 7985
        val DEFAULT_RPC_BIND = InetAddressPort("0.0.0.0", DEFAULT_RPC_PORT)
        val DEFAULT_BLOCKS_PER_EON = ContractConstructArgs.TEST_BLOCKS_PER_EON
        val DEFAULT_CONNECTOR = URI("${InMemoryChainProvider.scheme}:test")
        val DEFAULT_DATA_DIR = "${System.getProperty("user.home")}/.starcoin/hub/"

        fun loadConfig(dataDir: File): Config {
            val properties = Properties()
            if (dataDir.exists()) {
                if (!dataDir.isDirectory) {
                    throw IllegalArgumentException("Data dir $dataDir exists, and is not a directory.")
                }
            } else {
                Preconditions.checkArgument(dataDir.mkdirs(), "Make data dir $dataDir fail.")
            }
            val configFile = File(dataDir, "hub.conf")

            if (configFile.exists()) {
                configFile.inputStream().use { properties.load(it) }
            } else {
                LOG.info("Config file ${configFile.absolutePath} is not exist, load default config.")
                Config::class.java.classLoader.getResource("hub.conf").openStream().use { properties.load(it) }
            }
            return Config(properties, dataDir, configFile)
        }

        fun configurationForUNIT(): Config {
            val properties = Properties()
            properties[Config::network.name] = Network.UINT.name
            properties[Config::rpcBind.name] = InetAddressPort.random().toString()
            properties[Config::connector.name] = DEFAULT_CONNECTOR.toASCIIString()
            val dataDir = Files.createTempDir()
            dataDir.deleteOnExit()
            return Config(properties, dataDir, File(dataDir, "hub.conf"))
        }
    }
}
