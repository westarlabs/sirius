package org.starcoin.sirius.wallet.core

import io.grpc.stub.StreamObserver
import org.starcoin.proto.Starcoin
import java.util.*

class HubObserver : StreamObserver<Starcoin.HubEvent> {

    private val consumerList = ArrayList<(Starcoin.HubEvent)->Unit>()

    override fun onNext(value: Starcoin.HubEvent) {
        for (protoHubEventConsumer in consumerList) {
            protoHubEventConsumer(value)
        }
    }

    override fun onError(t: Throwable) {}

    override fun onCompleted() {}

    fun addConsumer(consumer: (Starcoin.HubEvent)->Unit) {
        this.consumerList.add(consumer)
    }
}
