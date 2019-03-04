package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.Deadline
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
import java.util.concurrent.TimeUnit

class HubServiceStub(private val originStub: HubServiceGrpc.HubServiceBlockingStub, val timeoutMillis: Long = 2000) :
    HubService {

    val stub: HubServiceGrpc.HubServiceBlockingStub
        get() = originStub.withDeadline(Deadline.after(timeoutMillis, TimeUnit.MILLISECONDS))

    override var hubMaliciousFlag: EnumSet<HubService.HubMaliciousFlag>
        get() = HubService.HubMaliciousFlag.of(stub.getMaliciousFlags(Empty.getDefaultInstance()))
        set(value) {
            stub.setMaliciousFlags(HubService.HubMaliciousFlag.toProto(value))
        }

    override val hubInfo: HubInfo
        get() = stub.getHubInfo(Empty.getDefaultInstance()).toSiriusObject()

    override fun start() {
    }

    override fun stop() {
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

    override fun queryNewTransfer(address: Address): List<OffchainTransaction> = catchEx<List<OffchainTransaction>> {
        stub.queryNewTransfer(address.toProto())
            .txsList.map { it.toSiriusObject<Starcoin.OffchainTransaction, OffchainTransaction>() }
    }!!

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
                originStub
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
                originStub
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

    override fun resetHubMaliciousFlag(): EnumSet<HubService.HubMaliciousFlag> {
        return HubService.HubMaliciousFlag.of(stub.resetMaliciousFlags(Empty.getDefaultInstance()))
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
