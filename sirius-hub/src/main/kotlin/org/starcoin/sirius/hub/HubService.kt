package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import java.util.*

interface HubService {
    var hubMaliciousFlag: EnumSet<Hub.HubMaliciousFlag>
    val hubInfo: HubInfo
    fun start()
    fun registerParticipant(participant: Participant, initUpdate: Update): Update
    fun sendNewTransfer(iou: IOU)
    fun receiveNewTransfer(receiverIOU: IOU)
    fun queryNewTransfer(address: Address): OffchainTransaction?
    fun querySignedUpdate(address: Address): Update?
    fun querySignedUpdate(eon: Int, blockAddress: Address): Update?
    fun getProof(address: Address): AMTreeProof?
    fun getProof(eon: Int, blockAddress: Address): AMTreeProof?
    fun watch(address: Address): ReceiveChannel<HubEvent>
    fun watchHubRoot(): ReceiveChannel<HubEvent>
    fun getHubAccount(address: Address): HubAccount?
    fun resetHubMaliciousFlag(): EnumSet<Hub.HubMaliciousFlag>
}
