package org.starcoin.sirius.hub

import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.hub.Hub.HubMaliciousFlag
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.HubContract
import java.security.PublicKey
import java.util.*
import java.util.concurrent.BlockingQueue

class HubService<T : ChainTransaction>(
    private val hubKey: CryptoKey,
    blocksPerEon: Int,
    chain: Chain<T, Block<T>, HubContract>
) {

    var hubMaliciousFlag: EnumSet<HubMaliciousFlag>
        get() = hub.hubMaliciousFlag
        set(flags) {
            hub.hubMaliciousFlag = flags
        }

    private val hub: Hub

    val currentEon: Eon?
        get() = hub.currentEon()

    val hubPublicKey: PublicKey
        get() = this.hubKey.keyPair.public

    val stateRoot: AMTreeNode
        get() = this.hub.stateRoot

    val hubInfo: HubInfo
        get() = this.hub.hubInfo

    init {

        this.hub = HubImpl(hubKey, blocksPerEon, chain)
    }

    fun start() {
        this.hub.start()
    }

    fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        return this.hub.registerParticipant(participant, initUpdate)
    }

    fun deposit(participant: Address, amount: Long) {
        this.hub.deposit(participant, amount)
    }

    fun sendNewTransfer(iou: IOU) {
        this.hub.sendNewTransfer(iou)
    }

    fun receiveNewTransfer(receiverIOU: IOU) {
        this.hub.receiveNewTransfer(receiverIOU)
    }

    fun queryNewTransfer(blockAddress: Address): OffchainTransaction? {
        return this.hub.queryNewTransfer(blockAddress)
    }

    fun querySignedUpdate(blockAddress: Address): Update? {
        val hubAccount = this.hub.getHubAccount(blockAddress)
        return hubAccount?.update
    }

    fun querySignedUpdate(eon: Int, blockAddress: Address): Update? {
        val hubAccount = this.hub.getHubAccount(eon, blockAddress)
        return hubAccount?.update
    }

    fun getProof(blockAddress: Address): AMTreeProof? {
        return hub.getProof(blockAddress)
    }

    fun getProof(eon: Int, blockAddress: Address): AMTreeProof? {
        return hub.getProof(eon, blockAddress)
    }

    fun watch(address: Address): BlockingQueue<HubEvent> {
        return hub.watch(address)
    }

    fun watch(predicate: (HubEvent) -> Boolean): BlockingQueue<HubEvent> {
        return hub.watchByFilter(predicate)
    }

    fun getHubAccount(blockAddress: Address): HubAccount? {
        return this.hub.getHubAccount(blockAddress)
    }

    fun resetHubMaliciousFlag(): EnumSet<HubMaliciousFlag> {
        return this.hub.resetHubMaliciousFlag()
    }
}
