package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.util.WithLogging
import java.util.*

class HubServiceStub(val stub: HubServiceGrpc.HubServiceBlockingStub) : HubService {

    override var hubMaliciousFlag: EnumSet<Hub.HubMaliciousFlag>
        get() = Hub.HubMaliciousFlag.of(stub.getMaliciousFlags(Empty.getDefaultInstance()))
        set(value) {
            stub.setMaliciousFlags(Hub.HubMaliciousFlag.toProto(value))
        }

    override val hubInfo: HubInfo
        get() = stub.getHubInfo(Empty.getDefaultInstance()).toSiriusObject()

    override fun start() {
    }

    override fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        return stub.registerParticipant(
            Starcoin.RegisterParticipantRequest.newBuilder().setParticipant(participant.toProto<Starcoin.Participant>()).setUpdate(
                initUpdate.toProto<Starcoin.Update>()
            ).build()
        ).toSiriusObject()
    }

    override fun sendNewTransfer(iou: IOU) {
        val resp = stub.sendNewTransfer(iou.toProto())
        assert(resp.succ)
    }

    override fun receiveNewTransfer(receiverIOU: IOU) {
        val resp = stub.receiveNewTransfer(receiverIOU.toProto())
        assert(resp.succ)
    }

    override fun queryNewTransfer(address: Address) = catchEx<OffchainTransaction> {
        stub.queryNewTransfer(address.toProto()).toSiriusObject()
    }

    override fun querySignedUpdate(address: Address) = catchEx<Update> {
        stub.queryUpdate(address.toProto()).toSiriusObject()
    }

    override fun querySignedUpdate(eon: Int, blockAddress: Address) = catchEx<Update> {
        stub.queryUpdateWithEon(Starcoin.BlockAddressAndEon.newBuilder().setEon(eon).setAddress(blockAddress.toByteString()).build())
            .toSiriusObject()
    }

    override fun getProof(address: Address): AMTreeProof? = catchEx<AMTreeProof> {
        stub.getProof(address.toProto()).toSiriusObject()
    }

    override fun getProof(eon: Int, blockAddress: Address) = catchEx<AMTreeProof> {
        stub.getProofWithEon(Starcoin.BlockAddressAndEon.newBuilder().setEon(eon).setAddress(blockAddress.toByteString()).build())
            .toSiriusObject()
    }


    override fun watch(address: Address): ReceiveChannel<HubEvent> {
        val channel = Channel<HubEvent>()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                stub
                    .watch(address.toProto())
                    .forEachRemaining { protoHubEvent ->
                        launch { channel.send(protoHubEvent.toSiriusObject()) }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return channel
    }

    override fun watchHubRoot(): ReceiveChannel<HubRoot> {
        val channel = Channel<HubRoot>()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                stub
                    .watchHubRoot(Empty.getDefaultInstance())
                    .forEachRemaining { protoHubRoot ->
                        launch { channel.send(protoHubRoot.toSiriusObject()) }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return channel
    }

    override fun getHubAccount(address: Address) = catchEx<HubAccount> {
        stub.getHubAccount(address.toProto()).toSiriusObject()
    }

    override fun resetHubMaliciousFlag(): EnumSet<Hub.HubMaliciousFlag> {
        return Hub.HubMaliciousFlag.of(stub.resetMaliciousFlags(Empty.getDefaultInstance()))
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
