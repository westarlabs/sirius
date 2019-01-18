package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.util.WithLogging

class HubRpcServer<T : ChainTransaction, A : ChainAccount>(val hubService: HubService<T, A>) :
    HubServiceGrpc.HubServiceImplBase() {

    companion object : WithLogging()

    private fun <V> doResponse(responseObserver: StreamObserver<V>, action: () -> V?) {
        try {
            val result = action()
            result?.let {
                responseObserver.onNext(result)
                responseObserver.onCompleted()
            } ?: responseObserver.onError(StatusRuntimeException(Status.NOT_FOUND))
        } catch (e: Exception) {
            this.doResponseError(responseObserver, e)
        }

    }

    private fun doResponseError(responseObserver: StreamObserver<*>, e: Exception) {
        LOG.severe(e.message)
        val exception: StatusRuntimeException
        if (e is StatusRuntimeException) {
            exception = e
        } else if (e is IllegalArgumentException) {
            exception = StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e))
            e.printStackTrace()
        } else if (e is IllegalStateException) {
            exception = StatusRuntimeException(Status.INTERNAL.withCause(e))
            e.printStackTrace()
        } else {
            exception = StatusRuntimeException(Status.UNKNOWN.withCause(e))
            e.printStackTrace()
        }
        responseObserver.onError(exception)
    }


    override fun registerParticipant(
        request: Starcoin.RegisterParticipantRequest,
        responseObserver: StreamObserver<Starcoin.Update>
    ) {
        this.doResponse(
            responseObserver
        ) {
            val participant = Participant.parseFromProtoMessage(request.participant)
            val update = Update.parseFromProtoMessage(request.update)
            hubService.registerParticipant(participant, update).toProto()
        }
    }

    override fun sendNewTransfer(request: Starcoin.IOU, responseObserver: StreamObserver<Starcoin.SuccResponse>) {
        this.doResponse(
            responseObserver
        ) {
            hubService.sendNewTransfer(IOU.parseFromProtoMessage(request))
            Starcoin.SuccResponse.newBuilder().setSucc(true).build()
        }
    }

    override fun queryNewTransfer(
        request: Starcoin.ProtoBlockAddress,
        responseObserver: StreamObserver<Starcoin.OffchainTransaction>
    ) {
        this.doResponse(
            responseObserver
        ) { this.hubService.queryNewTransfer(Address.wrap(request.address))?.toProto() }
    }

    override fun receiveNewTransfer(request: Starcoin.IOU, responseObserver: StreamObserver<Starcoin.SuccResponse>) {
        this.doResponse(
            responseObserver
        ) {
            this.hubService.receiveNewTransfer(IOU.parseFromProtoMessage(request))
            Starcoin.SuccResponse.newBuilder().setSucc(true).build()
        }
    }

    override fun queryUpdate(request: Starcoin.ProtoBlockAddress, responseObserver: StreamObserver<Starcoin.Update>) {
        doResponse(responseObserver) { hubService.querySignedUpdate(Address.wrap(request.address))?.toProto() }
    }

    override fun queryUpdateWithEon(
        request: Starcoin.BlockAddressAndEon,
        responseObserver: StreamObserver<Starcoin.Update>
    ) {
        doResponse(
            responseObserver
        ) {
            hubService.querySignedUpdate(
                request.getEon(), Address.wrap(request.getAddress())
            )?.toProto()
        }
    }

    override fun getProof(
        request: Starcoin.ProtoBlockAddress,
        responseObserver: StreamObserver<Starcoin.AMTreeProof>
    ) {
        this.doResponse(responseObserver) { hubService.getProof(Address.wrap(request.address))?.toProto() }
    }

    override fun getProofWithEon(
        request: Starcoin.BlockAddressAndEon,
        responseObserver: StreamObserver<Starcoin.AMTreeProof>
    ) {
        this.doResponse(
            responseObserver
        ) { hubService.getProof(request.eon, Address.wrap(request.address))?.toProto() }
    }

    override fun getHubInfo(request: Empty?, responseObserver: StreamObserver<Starcoin.HubInfo>) {
        this.doResponse(responseObserver) {
            hubService.hubInfo.toProto()
        }
    }

    override fun watch(request: Starcoin.ProtoBlockAddress, responseObserver: StreamObserver<Starcoin.HubEvent>) {
        val queue = this.hubService.watch(Address.wrap(request.address))
        //TODO optimize
        try {
            while (true) {
                val event = queue.take()
                responseObserver.onNext(event.toProto())
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    override fun watchHubRoot(request: Empty?, responseObserver: StreamObserver<Starcoin.HubRoot>) {
        val queue = this.hubService.watch { event -> event.type === HubEventType.NEW_HUB_ROOT }
        try {
            while (true) {
                val event = queue.take()
                responseObserver.onNext(event.getPayload<HubRoot>().toProto())
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    override fun getHubAccount(
        request: Starcoin.ProtoBlockAddress,
        responseObserver: StreamObserver<Starcoin.HubAccount>
    ) {
        this.doResponse(
            responseObserver
        ) { this.hubService.getHubAccount(Address.wrap(request.address))?.toProto() }
    }


    override fun setMaliciousFlags(
        request: Starcoin.HubMaliciousFlags,
        responseObserver: StreamObserver<Starcoin.HubMaliciousFlags>
    ) {

        doResponse(responseObserver) {
            val enumSet = Hub.HubMaliciousFlag.of(request)
            this.hubService.hubMaliciousFlag = enumSet
            Hub.HubMaliciousFlag.toProto(this.hubService.hubMaliciousFlag)
        }
    }

    override fun getMaliciousFlags(request: Empty?, responseObserver: StreamObserver<Starcoin.HubMaliciousFlags>) {
        doResponse(responseObserver) { Hub.HubMaliciousFlag.toProto(this.hubService.hubMaliciousFlag) }
    }

    override fun resetMaliciousFlags(request: Empty?, responseObserver: StreamObserver<Starcoin.HubMaliciousFlags>) {
        doResponse(responseObserver) { Hub.HubMaliciousFlag.toProto(this.hubService.resetHubMaliciousFlag()) }
    }
}
