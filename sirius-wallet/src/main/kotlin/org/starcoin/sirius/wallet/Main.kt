package org.starcoin.sirius.wallet

import com.google.common.base.Preconditions
import jline.console.ConsoleReader
import jline.console.completer.ArgumentCompleter
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.wallet.command.CliCommands
import org.starcoin.sirius.wallet.command.WalletCommand
import org.starcoin.sirius.wallet.core.ChannelManager
import org.starcoin.sirius.wallet.core.HubStatus
import org.starcoin.sirius.wallet.core.Wallet
import org.starcoin.sirius.wallet.core.store.FileStore
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


class Main {

    private val PRIVATE_KEY_FILENAME = "private"
    private val PUBLIC_KEY_FILENAME = "public"
    private val ADDRESS_FILENAME = "address"

    fun main(args : Array<String>) {
        var name = ""
        if (args.size <= 0) {
            System.err.println("cli app should have name!")
            System.exit(1)
        } else {
            name = args[0]
        }
        try {
            val properties = loadConfig()
            val hubAddr = properties.getProperty("hub_addr")
            val contractAddr = properties.getProperty("contract_addr")
            val chainAddr = properties.getProperty("chain_addr")

            var contractAddress = contractAddr.toByteArray()
            val reader = ConsoleReader()
            reader.prompt = String.format("%s>", name)

            val cmd = CommandLine(CliCommands(reader))
            val channelManager = ChannelManager(
                InetAddressPort.valueOf(hubAddr), InetAddressPort.valueOf(contractAddr)
            )

            val key = generateKey(name)
            val chain = EthereumChain(chainAddr)
            var store = FileStore(this.getWalletDir(name).path, HubStatus::class.java)
            //var wallet=Wallet(Address.wrap(contractAddress),channelManager,chain,store,key)

            //cmd.addSubcommand("wallet", WalletCommand(wallet))

            var line: String

            while (reader.readLine().let {line=it;it!=null}) {
                val list = ArgumentCompleter.WhitespaceArgumentDelimiter().delimit(line, line.length)

                cmd.registerConverter(Address::class.java!!, Address.Companion::wrap)
                    .parseWithHandlers(
                        CommandLine.RunLast(),
                        object : CommandLine.DefaultExceptionHandler<List<Any>>() {
                            override fun handleExecutionException(
                                ex: CommandLine.ExecutionException?, parseResult: CommandLine.ParseResult?
                            ): List<Any> {
                                super.err().println(ex!!.message)
                                // TODO exception define and handler
                                ex.printStackTrace(super.err())
                                ex.commandLine.usage(super.out(), super.ansi())
                                return emptyList()
                            }
                        },
                        *list.arguments
                    )
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }

    @Throws(IOException::class)
    private fun loadConfig(): Properties {
        val prop = Properties()
        var inputStream: InputStream? = null
        val configFile = File(
            System.getProperty("user.home"),
            ".starcoin" + File.separator + "liq" + File.separator + "conf.properties"
        )

        if (configFile.exists()) {
            inputStream = FileInputStream(configFile)
        } else {
            val path = Paths.get("./conf.properties")
            if (Files.exists(path)) {
                inputStream = FileInputStream("./conf.properties")
            } else {
                inputStream = Main::class.java!!.getClassLoader().getResourceAsStream("conf.properties")
            }
        }
        prop.load(inputStream)
        return prop
    }

    fun generateKey(name: String):CryptoKey {
        val walletDir = getWalletDir(name)
        val keyFile = File(walletDir, PRIVATE_KEY_FILENAME)
        var key: CryptoKey
        if (keyFile.exists()) {
            key=CryptoService.loadCryptoKey(Files.readAllBytes(keyFile.toPath()))
        } else {
            key = CryptoService.generateCryptoKey()
            Files.write(keyFile.toPath(),key.toBytes())
        }
        return key
    }

    fun getWalletDir(name:String): File {
        val walletDir = genWalletDir(name)
        Preconditions.checkState(
            walletDir.exists(),
            "wallet " + walletDir.absolutePath + " is not exist, please create first.")
        return walletDir
    }

    fun genWalletDir(name:String): File {
        return File(
            System.getProperty("user.home"),
            ".starcoin" + File.separator + "liq" + File.separator + name
        )
    }

}