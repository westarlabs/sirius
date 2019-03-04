package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import java.util.*

interface Hub {

    var hubMaliciousFlag: EnumSet<HubService.HubMaliciousFlag>

    val stateRoot: AMTreeNode

    val hubInfo: HubInfo

    //return previous Flags
    suspend fun resetHubMaliciousFlag(): EnumSet<HubService.HubMaliciousFlag>

    fun start()

    fun stop()

    suspend fun registerParticipant(participant: Participant, initUpdate: Update): Update

    suspend fun deposit(participant: Address, amount: Long)

    suspend fun getHubAccount(address: Address): HubAccount?

    suspend fun getHubAccount(eon: Int, address: Address): HubAccount?

    suspend fun sendNewTransfer(iou: IOU)

    suspend fun receiveNewTransfer(receiverIOU: IOU)

    suspend fun queryNewTransfer(address: Address): List<OffchainTransaction>

    suspend fun getProof(address: Address): AMTreeProof?

    suspend fun getProof(eon: Int, address: Address): AMTreeProof?

    fun currentEon(): Eon?

    suspend fun watch(address: Address): ReceiveChannel<HubEvent> {
        return this.watch { event -> event.isPublicEvent || event.address == address }
    }

    suspend fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent>

}
