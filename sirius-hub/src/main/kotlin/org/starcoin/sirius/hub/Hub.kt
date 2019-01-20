package org.starcoin.sirius.hub

import com.google.common.eventbus.Subscribe
import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.HubMaliciousFlags
import org.starcoin.sirius.core.*
import java.util.*
import java.util.stream.Collectors

interface Hub {

    var hubMaliciousFlag: EnumSet<HubMaliciousFlag>

    val stateRoot: AMTreeNode

    val hubInfo: HubInfo

    //return previous Flags
    fun resetHubMaliciousFlag(): EnumSet<HubMaliciousFlag>

    class HubEventListener(val eventLambda: (HubEvent) -> Unit) {

        @Subscribe
        fun onEvent(event: HubEvent) {
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

    fun queryNewTransfer(address: Address): OffchainTransaction?

    fun getProof(address: Address): AMTreeProof?

    fun getProof(eon: Int, address: Address): AMTreeProof?

    fun currentEon(): Eon?

    fun watch(address: Address): ReceiveChannel<HubEvent> {
        return this.watch { event -> event.isPublicEvent || event.address == address }
    }

    fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent>

    enum class HubMaliciousFlag constructor(private val protoFlag: Starcoin.HubMaliciousFlag) :
        SiriusEnum<Starcoin.HubMaliciousFlag> {
        STEAL_DEPOSIT(Starcoin.HubMaliciousFlag.STEAL_DEPOSIT),
        STEAL_WITHDRAWAL(Starcoin.HubMaliciousFlag.STEAL_WITHDRAWAL),
        STEAL_TRANSACTION(Starcoin.HubMaliciousFlag.STEAL_TRANSACTION),
        STEAL_TRANSACTION_IOU(Starcoin.HubMaliciousFlag.STEAL_TRANSACTION_IOU);


        override fun toProto(): Starcoin.HubMaliciousFlag {
            return protoFlag
        }

        companion object {
            val ALL_OPTS = EnumSet.allOf(HubMaliciousFlag::class.java)

            fun valueOf(number: Int): HubMaliciousFlag {
                for (flag in HubMaliciousFlag.values()) {
                    if (flag.protoFlag.number == number) {
                        return flag
                    }
                }
                throw IllegalArgumentException("unsupported MaliciousFlag $number")
            }

            fun of(flags: HubMaliciousFlags): EnumSet<HubMaliciousFlag> {
                return EnumSet.copyOf(
                    flags
                        .flagsList
                        .stream()
                        .map { protoHubMaliciousFlag -> valueOf(protoHubMaliciousFlag.number) }
                        .collect(Collectors.toList<HubMaliciousFlag>()))
            }

            fun toProto(flags: EnumSet<HubMaliciousFlag>): HubMaliciousFlags {
                return HubMaliciousFlags.newBuilder()
                    .addAllFlags(
                        flags.stream().map<Starcoin.HubMaliciousFlag> { it.toProto() }.collect(
                            Collectors.toList()
                        )
                    )
                    .build()
            }
        }
    }
}
