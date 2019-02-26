package org.starcoin.sirius.wallet

import com.google.common.base.Preconditions
import jline.console.ConsoleReader
import jline.console.completer.ArgumentCompleter
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.wallet.command.CliCommands
import org.starcoin.sirius.wallet.command.WalletCommand
import org.starcoin.sirius.wallet.core.ChannelManager
import org.starcoin.sirius.wallet.core.Wallet
import org.web3j.crypto.WalletUtils
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {

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
        val chainAddr = properties.getProperty("chain_addr")
        val contractAddr = properties.getProperty("contract_addr")
        val keyStoreFilePath = properties.getProperty("key_store")
        val password = properties.getProperty("password")

        val reader = ConsoleReader()
        reader.prompt = String.format("%s>", name)

        val cmd = CommandLine(CliCommands(reader))
        val channelManager = ChannelManager(InetAddressPort.valueOf(hubAddr))

        val chain = EthereumChain(chainAddr)
        var account = loadAccount(keyStoreFilePath, password, chain)

        var wallet = Wallet(Address.wrap(contractAddr), channelManager, chain, account,getWalletDir(name).path)

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
            inputStream = object {}.javaClass.getClassLoader().getResourceAsStream("conf.properties")
        }
    }
    prop.load(inputStream)
    return prop
}

fun getWalletDir(name: String): File {
    val walletDir = genWalletDir(name)
    Preconditions.checkState(
        walletDir.exists(),
        "wallet " + walletDir.absolutePath + " is not exist, please create first."
    )
    return walletDir
}

fun genWalletDir(name: String): File {
    return File(
        System.getProperty("user.home"),
        ".starcoin" + File.separator + "liq" + File.separator + name
    )
}

fun loadAccount(path: String, password: String, chain: EthereumChain): EthereumAccount {
    val credentials = WalletUtils.loadCredentials(
        password,
        File(path)
    )
    val cryptoKey = CryptoService.loadCryptoKey(credentials.ecKeyPair.privateKey.toByteArray())
    return EthereumAccount(cryptoKey, AtomicLong(chain.getNonce(cryptoKey.address).longValueExact()))
}

