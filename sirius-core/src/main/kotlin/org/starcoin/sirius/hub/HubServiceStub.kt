package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.Deadline
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.util.WithLogging
import java.util.*
import java.util.concurrent.TimeUnit

class DeferredStreamObserver<V>(val deferred: CompletableDeferred<V> = CompletableDeferred()) : StreamObserver<V>,
    Deferred<V> by deferred {
    override fun onNext(value: V) {
        deferred.complete(value)
    }

    override fun onError(t: Throwable) {
        deferred.completeExceptionally(t)
    }

    override fun onCompleted() {
    }
}

class StreamObserverChannel<V>(val channel: Channel<V> = Channel()) : StreamObserver<V>, Channel<V> by channel {
    var error: Throwable? = null

    override fun onNext(value: V) {
        GlobalScope.launch(Dispatchers.IO) {
            channel.send(value)
        }
    }

    override fun onError(t: Throwable?) {
        this.error = t
    }

    override fun onCompleted() {
        channel.close(error)
    }
}

class HubServiceStub(private val channel: ManagedChannel, private val timeoutMillis: Long = 4000) :
    HubService {

    private val originStub: HubServiceGrpc.HubServiceStub = HubServiceGrpc.newStub(channel)
    private val stub: HubServiceGrpc.HubServiceStub
        get() = originStub.withDeadline(Deadline.after(timeoutMillis, TimeUnit.MILLISECONDS))

    override var hubMaliciousFlag: EnumSet<HubService.HubMaliciousFlag>
        get() = runBlocking {
            HubService.HubMaliciousFlag.of(call(Empty.getDefaultInstance(), stub::getMaliciousFlags))
        }
        set(value) = runBlocking<Unit> {
            call(HubService.HubMaliciousFlag.toProto(value), stub::setMaliciousFlags)
        }

    override val hubInfo: HubInfo
        get() = runBlocking {
            HubInfo.parseFromProtoMessage(DeferredStreamObserver<Starcoin.HubInfo>().also {
                stub.getHubInfo(
                    Empty.getDefaultInstance(),
                    it
                )
            }.await())
        }

    override fun start() {
    }

    override fun stop() {
        channel.shutdownNow()
    }

    private suspend inline fun <I, O> call(input: I, method: (I, StreamObserver<O>) -> Unit): O {
        return DeferredStreamObserver<O>().also { method(input, it) }.await()
    }

    override suspend fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        return call(
            Starcoin.RegisterParticipantRequest.newBuilder().setParticipant(participant.toProto<Starcoin.Participant>()).setUpdate(
                initUpdate.toProto<Starcoin.Update>()
            ).build(), stub::registerParticipant
        ).toSiriusObject()
    }

    override suspend fun sendNewTransfer(iou: IOU) {
        call(iou.toProto(), stub::sendNewTransfer).apply { assert(this.succ) }
    }

    override suspend fun receiveNewTransfer(receiverIOU: IOU) {
        call(receiverIOU.toProto(), stub::receiveNewTransfer).apply { assert(this.succ) }
    }

    override suspend fun queryNewTransfer(address: Address): List<OffchainTransaction> =
        catchEx<List<OffchainTransaction>> {
            call(
                address.toProto(),
                stub::queryNewTransfer
            ).txsList.map { it.toSiriusObject<Starcoin.OffchainTransaction, OffchainTransaction>() }
        }!!

    override suspend fun querySignedUpdate(address: Address) = catchEx<Update> {
        call(address.toProto(), stub::queryUpdate).toSiriusObject()
    }

    override suspend fun querySignedUpdate(eon: Int, blockAddress: Address) = catchEx<Update> {
        call(
            Starcoin.BlockAddressAndEon.newBuilder().setEon(eon).setAddress(blockAddress.toByteString()).build(),
            stub::queryUpdateWithEon
        ).toSiriusObject()
    }

    override suspend fun getProof(address: Address): AMTreeProof? = catchEx<AMTreeProof> {
        call(address.toProto(), stub::getProof).toSiriusObject()
    }

    override suspend fun getProof(eon: Int, blockAddress: Address): AMTreeProof? = catchEx<AMTreeProof> {
        return call(
            Starcoin.BlockAddressAndEon.newBuilder().setEon(eon).setAddress(blockAddress.toByteString()).build(),
            stub::getProofWithEon
        ).toSiriusObject()
    }


    override suspend fun watch(address: Address): ReceiveChannel<HubEvent> {
        val channel = StreamObserverChannel<Starcoin.HubEvent>()
        originStub
            .watch(address.toProto(), channel)
        return channel.map { it.toSiriusObject<Starcoin.HubEvent, HubEvent>() }
    }

    override suspend fun watchHubRoot(): ReceiveChannel<HubRoot> {
        val channel = StreamObserverChannel<Starcoin.HubRoot>()
        originStub
            .watchHubRoot(Empty.getDefaultInstance(), channel)
        return channel.map { it.toSiriusObject<Starcoin.HubRoot, HubRoot>() }
    }

    override suspend fun getHubAccount(address: Address) = catchEx<HubAccount> {
        call(address.toProto(), stub::getHubAccount).toSiriusObject()
    }

    override suspend fun resetHubMaliciousFlag(): EnumSet<HubService.HubMaliciousFlag> {
        return HubService.HubMaliciousFlag.of(call(Empty.getDefaultInstance(), stub::resetMaliciousFlags))
    }


    inline fun <reified T> catchEx(block: () -> T): T? {
        return try {
            block()
        } catch (e: StatusRuntimeException) {
            when (e.status.code) {
                Status.Code.NOT_FOUND -> null
                else -> throw e
            }
        }
    }

    companion object : WithLogging()
}
