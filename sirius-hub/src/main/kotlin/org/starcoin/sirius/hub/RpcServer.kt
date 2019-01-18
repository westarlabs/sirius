package org.starcoin.sirius.hub


interface RpcServer<S> {

    fun start()

    fun stop()

    fun registerService(service: S)

    //fun call(serviceName: String, methodName: String, request: ByteBuffer): ByteBuffer

    @Throws(InterruptedException::class)
    fun awaitTermination()
}
