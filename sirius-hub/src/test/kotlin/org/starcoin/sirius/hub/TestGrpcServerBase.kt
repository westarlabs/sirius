package org.starcoin.sirius.hub

import io.grpc.BindableService
import org.junit.After
import org.junit.Before
import kotlin.properties.Delegates

abstract class TestGrpcServerBase {

    var grpcServer: GrpcServer by Delegates.notNull()

    var configuration: Config by Delegates.notNull()

    @Before
    fun before() {
        configuration = Config.configurationForUNIT()
        this.grpcServer = GrpcServer(configuration)
        this.grpcServer.registerService(this.createService())
        this.grpcServer.start()
    }

    protected abstract fun createService(): BindableService

    @After
    fun after() {
        this.grpcServer.stop()
    }
}
