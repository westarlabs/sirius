package org.starcoin.sirius.hub

import com.google.common.eventbus.Subscribe
import org.starcoin.proto.Starcoin.ProtoHubMaliciousFlag
import org.starcoin.proto.Starcoin.ProtoHubMaliciousFlags
import org.starcoin.sirius.core.*
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.stream.Collectors

interface Hub {

    var hubMaliciousFlag: EnumSet<MaliciousFlag>

    val stateRoot: AMTNode

    val hubInfo: HubInfo

    //return previous Flags
    fun resetHubMaliciousFlag(): EnumSet<MaliciousFlag>

    class HubEventListener(val eventLambda: (HubEvent<SiriusObject>) -> Unit) {

        @Subscribe
        fun onEvent(event: HubEvent<SiriusObject>) {
            eventLambda(event)
        }
    }

    fun start()

    fun registerParticipant(participant: Participant, initUpdate: Update): Update

    fun deposit(participant: Address, amount: Long)

    fun getHubAccount(address: Address): HubAccount?

    fun getHubAccount(eon: Int, address: Address): HubAccount?

    // TODO
    @Deprecated("")
    fun transfer(transaction: OffchainTransaction, fromUpdate: Update, toUpdate: Update): Array<Update>

    fun sendNewTransfer(iou: IOU)

    fun receiveNewTransfer(receiverIOU: IOU)

    fun queryNewTransfer(blockAddress: Address): OffchainTransaction?

    fun getProof(blockAddress: Address): AMTPath?

    fun getProof(eon: Int, blockAddress: Address): AMTPath?

    fun currentEon(): Eon?

    fun watch(address: Address): BlockingQueue<HubEvent<SiriusObject>> {
        return this.watchByFilter { event -> event.isPublicEvent || event.address == address }
    }

    fun watchByFilter(predicate: (HubEvent<SiriusObject>) -> Boolean): BlockingQueue<HubEvent<SiriusObject>>

    fun watch(listener: HubEventListener)

    fun onBlock(blockInfo: BlockInfo)

    enum class MaliciousFlag private constructor(private val protoFlag: ProtoHubMaliciousFlag) :
        ProtoEnum<ProtoHubMaliciousFlag> {
        STEAL_DEPOSIT(ProtoHubMaliciousFlag.PROTO_STEAL_DEPOSIT),
        STEAL_WITHDRAWAL(ProtoHubMaliciousFlag.PROTO_STEAL_WITHDRAWAL),
        STEAL_TRANSACTION(ProtoHubMaliciousFlag.PROTO_STEAL_TRANSACTION),
        STEAL_TRANSACTION_IOU(ProtoHubMaliciousFlag.PROTO_STEAL_TRANSACTION_IOU);


        override fun toProto(): ProtoHubMaliciousFlag {
            return protoFlag
        }

        companion object {
            val ALL_OPTS = EnumSet.allOf(MaliciousFlag::class.java)

            fun valueOf(number: Int): MaliciousFlag {
                for (flag in MaliciousFlag.values()) {
                    if (flag.protoFlag.number == number) {
                        return flag
                    }
                }
                throw IllegalArgumentException("unsupported MaliciousFlag $number")
            }

            fun of(flags: ProtoHubMaliciousFlags): EnumSet<MaliciousFlag> {
                return EnumSet.copyOf(
                    flags
                        .flagsList
                        .stream()
                        .map { protoHubMaliciousFlag -> valueOf(protoHubMaliciousFlag.number) }
                        .collect(Collectors.toList<MaliciousFlag>()))
            }

            fun toProto(flags: EnumSet<MaliciousFlag>): ProtoHubMaliciousFlags {
                return ProtoHubMaliciousFlags.newBuilder()
                    .addAllFlags(
                        flags.stream().map<ProtoHubMaliciousFlag> { it.toProto() }.collect(
                            Collectors.toList()
                        )
                    )
                    .build()
            }
        }
    }
}
