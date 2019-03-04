package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.IOU
import org.starcoin.sirius.core.Participant
import org.starcoin.sirius.core.Update
import org.starcoin.sirius.util.WithLogging

class HubRpcService(val hubService: HubService) :
    HubServiceGrpc.HubServiceImplBase() {

    companion object : WithLogging()

    private fun <V> doResponse(responseObserver: StreamObserver<V>, action: suspend () -> V?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = action()
                result?.let {
                    responseObserver.onNext(result)
                    responseObserver.onCompleted()
                } ?: responseObserver.onError(StatusRuntimeException(Status.NOT_FOUND))
            } catch (e: Exception) {
                doResponseError(responseObserver, e)
            }
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
        responseObserver: StreamObserver<Starcoin.OffchainTransactionList>
    ) {
        this.doResponse(
            responseObserver
        ) {
            Starcoin.OffchainTransactionList.newBuilder()
                .addAllTxs(this.hubService.queryNewTransfer(Address.wrap(request.address)).map { it.toProto<Starcoin.OffchainTransaction>() })
                .build()
        }
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
        GlobalScope.launch(Dispatchers.IO) {
            val queue = hubService.watch(Address.wrap(request.address))
            for (event in queue) {
                responseObserver.onNext(event.toProto())
            }
        }
    }

    override fun watchHubRoot(request: Empty?, responseObserver: StreamObserver<Starcoin.HubRoot>) {
        GlobalScope.launch(Dispatchers.IO) {
            val queue = hubService.watchHubRoot()
            for (event in queue) {
                responseObserver.onNext(event.toProto())
            }
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
            val enumSet = HubService.HubMaliciousFlag.of(request)
            this.hubService.hubMaliciousFlag = enumSet
            HubService.HubMaliciousFlag.toProto(this.hubService.hubMaliciousFlag)
        }
    }

    override fun getMaliciousFlags(request: Empty?, responseObserver: StreamObserver<Starcoin.HubMaliciousFlags>) {
        doResponse(responseObserver) { HubService.HubMaliciousFlag.toProto(this.hubService.hubMaliciousFlag) }
    }

    override fun resetMaliciousFlags(request: Empty?, responseObserver: StreamObserver<Starcoin.HubMaliciousFlags>) {
        doResponse(responseObserver) { HubService.HubMaliciousFlag.toProto(this.hubService.resetHubMaliciousFlag()) }
    }
}
