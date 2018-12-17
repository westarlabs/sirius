package org.starcoin.sirius.wallet.core

import io.grpc.stub.StreamObserver
import java.util.ArrayList
import java.util.function.Consumer
import org.starcoin.proto.Starcoin

class HubObserver : StreamObserver<Starcoin.ProtoHubEvent> {

    private val consumerList = ArrayList<Consumer<Starcoin.ProtoHubEvent>>()

    override fun onNext(value: Starcoin.ProtoHubEvent) {
        for (protoHubEventConsumer in consumerList) {
            protoHubEventConsumer.accept(value)
        }
    }

    override fun onError(t: Throwable) {}

    override fun onCompleted() {}

    fun addConsumer(consumer: Consumer<Starcoin.ProtoHubEvent>) {
        this.consumerList.add(consumer)
    }
}
