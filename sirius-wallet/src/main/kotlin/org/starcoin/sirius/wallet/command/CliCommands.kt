package org.starcoin.sirius.wallet.command

import java.io.PrintWriter
import jline.console.ConsoleReader
import picocli.CommandLine

/** Top-level command that just prints help.  */
@CommandLine.Command(
    name = "",
    description = arrayOf("Example interactive shell with completion"),
    footer = arrayOf("", "Press Ctl-D to exit."),
    mixinStandardHelpOptions = true,
    subcommands = arrayOf()
)
class CliCommands(internal val reader: ConsoleReader) : Runnable {
    internal val out: PrintWriter

    init {
        out = PrintWriter(reader.output)
    }

    override fun run() {
        out.println(CommandLine(this).usageMessage)
    }
}
