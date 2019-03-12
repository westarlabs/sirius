package org.starcoin.sirius.wallet

import io.grpc.netty.NettyChannelBuilder
import jline.console.ConsoleReader
import jline.console.completer.ArgumentCompleter
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.wallet.command.CliCommands
import org.starcoin.sirius.wallet.command.WalletCommand
import org.starcoin.sirius.wallet.core.ClientAccount
import org.starcoin.sirius.wallet.core.ResourceManager
import org.starcoin.sirius.wallet.core.Wallet
import org.web3j.crypto.WalletUtils
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level


fun main(args: Array<String>) {

    var name = ""
    if (args.size <= 0) {
        System.err.println("cli app should have name!")
        System.exit(1)
    } else {
        name = args[0]
    }
    try {
        val properties = loadConfig(name)
        val hubAddr = properties.getProperty("hub_addr")
        val chainAddr = properties.getProperty("chain_addr")
        val contractAddr = properties.getProperty("contract_addr")
        val keyStoreFilePath = properties.getProperty("key_store")
        val password = properties.getProperty("password")

        val reader = ConsoleReader()
        reader.prompt = String.format("%s>", name)

        val cmd = CommandLine(CliCommands(reader))
        ResourceManager.hubChannel = NettyChannelBuilder.forAddress(InetAddressPort.valueOf(hubAddr).toInetSocketAddress()).usePlaintext().build();
        ResourceManager.isTest=false


        val logDir = File(walletDir(name), "logs")
        assert(logDir.mkdir())
        WithLogging.addFileHandler(logDir.absolutePath + File.separator +"wallet%g.log")
        WithLogging.setLogLevel(Level.INFO)

        val chain = EthereumChain(chainAddr)
        var account = loadAccount(keyStoreFilePath, password, chain)

        var wallet = Wallet(Address.wrap(contractAddr), chain, ClientAccount(account,name))

        cmd.addSubcommand("wallet", WalletCommand(wallet))

        var line: String

        while (reader.readLine().let { line = it;it != null }) {
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

private fun loadConfig(name: String): Properties {
    val prop = Properties()
    var inputStream: InputStream? = null
    val configFile = File(
        System.getProperty("user.home"),
        ".sirius" + File.separator +name+ File.separator + "conf.properties"
    )

    if (configFile.exists()) {
        inputStream = FileInputStream(configFile)
    } else {
        val path = Paths.get("./conf.properties")
        if (Files.exists(path)) {
            inputStream = FileInputStream("./conf.properties")
        } else {
            inputStream = object {}.javaClass.getClassLoader().getResourceAsStream("conf.properties")
        }
    }
    prop.load(inputStream)
    return prop
}

private fun walletDir(name: String): File {
    val prop = Properties()
    var inputStream: InputStream? = null
    val path = File(
        System.getProperty("user.home"),
        ".sirius" + File.separator + name + File.separator
    )
    return path
}

fun loadAccount(path: String, password: String, chain: EthereumChain): EthereumAccount {
    val credentials = WalletUtils.loadCredentials(
        password,
        File(path)
    )
    val cryptoKey = CryptoService.loadCryptoKey(credentials.ecKeyPair.privateKey.toByteArray())
    return EthereumAccount(cryptoKey, AtomicLong(chain.getNonce(cryptoKey.address).longValueExact()))
}