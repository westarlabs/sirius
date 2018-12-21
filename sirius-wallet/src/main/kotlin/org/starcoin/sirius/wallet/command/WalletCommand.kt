package org.starcoin.sirius.wallet.command

import org.starcoin.sirius.wallet.core.Wallet
import picocli.CommandLine

@CommandLine.Command(
    name = "wallet",
    description = arrayOf("manage wallet"),
    mixinStandardHelpOptions = true,
    subcommands = arrayOf())
class WalletCommand(internal var wallet: Wallet) : Runnable {

    @CommandLine.ParentCommand
    var cliCommands: CliCommands? = null

    override fun run() {
    }
}