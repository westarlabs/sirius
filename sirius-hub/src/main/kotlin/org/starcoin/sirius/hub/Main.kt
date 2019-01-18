package org.starcoin.sirius.hub


fun main(args: Array<String>) {
    val configuration = Configuration.loadConfiguration()
    val hubServer = HubServer(configuration)
    hubServer.start()
    hubServer.awaitTermination()
}
