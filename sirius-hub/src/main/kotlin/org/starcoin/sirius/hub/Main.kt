package org.starcoin.sirius.hub

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.chain.ChainProvider
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.H2DBDataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.util.logging.Level


class ArgsConfig(parser: ArgParser) {
    val dataDir: File by parser.storing(
        "-d", "--data",
        help = "data dir"
    ) { File(this) }.default(File(Config.DEFAULT_DATA_DIR))
}

fun main(args: Array<String>) = runBlocking {
    val argConfig = ArgParser(args).parseInto(::ArgsConfig)
    val config = Config.loadConfig(argConfig.dataDir)
    val logDir = File(argConfig.dataDir, "logs")
    assert(logDir.mkdir())
    WithLogging.addFileHandler(logDir.absolutePath + File.separator + config.logPattern)
    WithLogging.setLogLevel(Level.INFO)
    val chain = ChainProvider.createChain(config.connector)
    val dataStoreFactory: DataStoreFactory
    if (chain is InMemoryChain) {
        dataStoreFactory = MapDataStoreFactory()
    } else {
        dataStoreFactory = H2DBDataStoreFactory(File(config.dataDir, "db"))
    }
    val hubServer = HubServer(config, chain, dataStoreFactory = dataStoreFactory)
    hubServer.start()
    hubServer.awaitTermination()
}
