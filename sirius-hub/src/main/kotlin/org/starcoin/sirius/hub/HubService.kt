package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import java.util.*

interface HubService {
    var hubMaliciousFlag: EnumSet<Hub.HubMaliciousFlag>
    val hubInfo: HubInfo
    fun start()
    fun registerParticipant(participant: Participant, initUpdate: Update): Update
    fun deposit(participant: Address, amount: Long)
    fun sendNewTransfer(iou: IOU)
    fun receiveNewTransfer(receiverIOU: IOU)
    fun queryNewTransfer(blockAddress: Address): OffchainTransaction?
    fun querySignedUpdate(blockAddress: Address): Update?
    fun querySignedUpdate(eon: Int, blockAddress: Address): Update?
    fun getProof(blockAddress: Address): AMTreeProof?
    fun getProof(eon: Int, blockAddress: Address): AMTreeProof?
    fun watch(address: Address): ReceiveChannel<HubEvent>
    fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent>
    fun getHubAccount(blockAddress: Address): HubAccount?
    fun resetHubMaliciousFlag(): EnumSet<Hub.HubMaliciousFlag>
}
