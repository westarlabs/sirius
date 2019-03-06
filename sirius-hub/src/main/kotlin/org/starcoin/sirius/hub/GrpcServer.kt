package org.starcoin.sirius.hub

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.netty.NettyServerBuilder
import org.starcoin.sirius.util.WithLogging
import java.util.*
import kotlin.properties.Delegates

class GrpcServer(val configuration: Config) : RpcServer<BindableService> {

    private var server: Server by Delegates.notNull()
    private val services: MutableMap<String, BindableService> = Collections.synchronizedMap(HashMap())

    override fun start() {
        val builder = this.serverBuilder()
        for (service in this.services.values) {
            builder.addService(service)
        }
        this.server = builder.build()
        server.start()

        LOG.info("Rpc Server started, listening on ${configuration.rpcBind}")
        Runtime.getRuntime()
            .addShutdownHook(
                object : Thread() {
                    override fun run() {
                        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                        System.err.println("*** shutting down gRPC server since JVM is shutting down")
                        this@GrpcServer.stop()
                        System.err.println("*** gRpc server shut down")
                    }
                })
    }

    private fun serverBuilder(): ServerBuilder<*> {
        return if (this.configuration.isUnitNetwork) {
            InProcessServerBuilder.forName(configuration.rpcBind.toString())
        } else {
            NettyServerBuilder.forAddress(this.configuration.rpcBind.toInetSocketAddress())
        }
    }

    override fun stop() {
        server.shutdown()
    }

    override fun registerService(service: BindableService) {
        this.services[service.bindService().serviceDescriptor.name] = service
    }


    @Throws(InterruptedException::class)
    override fun awaitTermination() {
        server.awaitTermination()
    }

    companion object : WithLogging() {
    }
}
