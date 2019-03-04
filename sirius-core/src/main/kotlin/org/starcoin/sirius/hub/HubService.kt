package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import java.util.*
import java.util.stream.Collectors

interface HubService {
    var hubMaliciousFlag: EnumSet<HubMaliciousFlag>
    val hubInfo: HubInfo
    fun start()
    fun registerParticipant(participant: Participant, initUpdate: Update): Update
    fun sendNewTransfer(iou: IOU)
    fun receiveNewTransfer(receiverIOU: IOU)
    fun queryNewTransfer(address: Address): List<OffchainTransaction>
    fun querySignedUpdate(address: Address): Update?
    fun querySignedUpdate(eon: Int, blockAddress: Address): Update?
    fun getProof(address: Address): AMTreeProof?
    fun getProof(eon: Int, blockAddress: Address): AMTreeProof?
    fun watch(address: Address): ReceiveChannel<HubEvent>
    fun watchHubRoot(): ReceiveChannel<HubRoot>
    fun getHubAccount(address: Address): HubAccount?
    fun resetHubMaliciousFlag(): EnumSet<HubMaliciousFlag>
    fun stop()

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

            fun of(flags: Starcoin.HubMaliciousFlags): EnumSet<HubMaliciousFlag> {
                if (flags.flagsList.size == 0) {
                    return EnumSet.noneOf(HubMaliciousFlag::class.java)
                }
                return EnumSet.copyOf(
                    flags
                        .flagsList
                        .stream()
                        .map { protoHubMaliciousFlag ->
                            valueOf(
                                protoHubMaliciousFlag.number
                            )
                        }
                        .collect(Collectors.toList<HubMaliciousFlag>()))
            }

            fun toProto(flags: EnumSet<HubMaliciousFlag>): Starcoin.HubMaliciousFlags {
                return Starcoin.HubMaliciousFlags.newBuilder()
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
