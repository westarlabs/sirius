package org.starcoin.sirius.wallet.core

import io.grpc.stub.StreamObserver
import org.starcoin.proto.Starcoin
import java.util.*
import java.util.function.Consumer

class HubObserver : StreamObserver<Starcoin.HubEvent> {

    private val consumerList = ArrayList<Consumer<Starcoin.HubEvent>>()

    override fun onNext(value: Starcoin.HubEvent) {
        for (protoHubEventConsumer in consumerList) {
            protoHubEventConsumer.accept(value)
        }
    }

    override fun onError(t: Throwable) {}

    override fun onCompleted() {}

    fun addConsumer(consumer: Consumer<Starcoin.HubEvent>) {
        this.consumerList.add(consumer)
    }
}
