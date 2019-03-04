package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import java.util.*

interface Hub {

    var hubMaliciousFlag: EnumSet<HubService.HubMaliciousFlag>

    val stateRoot: AMTreeNode

    val hubInfo: HubInfo

    //return previous Flags
    fun resetHubMaliciousFlag(): EnumSet<HubService.HubMaliciousFlag>

    fun start()

    fun stop()

    fun registerParticipant(participant: Participant, initUpdate: Update): Update

    fun deposit(participant: Address, amount: Long)

    fun getHubAccount(address: Address): HubAccount?

    fun getHubAccount(eon: Int, address: Address): HubAccount?

    fun sendNewTransfer(iou: IOU)

    fun receiveNewTransfer(receiverIOU: IOU)

    fun queryNewTransfer(address: Address): List<OffchainTransaction>

    fun getProof(address: Address): AMTreeProof?

    fun getProof(eon: Int, address: Address): AMTreeProof?

    fun currentEon(): Eon?

    fun watch(address: Address): ReceiveChannel<HubEvent> {
        return this.watch { event -> event.isPublicEvent || event.address == address }
    }

    fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent>

}
