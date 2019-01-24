package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import org.starcoin.sirius.hub.Hub.HubMaliciousFlag
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import java.util.*

class HubServiceImpl<T : ChainTransaction, A : ChainAccount>(
    private val owner: A,
    chain: Chain<T, out Block<T>, out A>,
    contract: HubContract<A>
) : HubService {

    private val hub: Hub

    init {
        this.hub = HubImpl(owner, chain, contract)
    }

    override var hubMaliciousFlag: EnumSet<HubMaliciousFlag>
        get() = hub.hubMaliciousFlag
        set(flags) {
            hub.hubMaliciousFlag = flags
        }

    override val hubInfo: HubInfo
        get() = this.hub.hubInfo

    override fun start() {
        this.hub.start()
    }

    override fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        return this.hub.registerParticipant(participant, initUpdate)
    }

    override fun deposit(participant: Address, amount: Long) {
        this.hub.deposit(participant, amount)
    }

    override fun sendNewTransfer(iou: IOU) {
        this.hub.sendNewTransfer(iou)
    }

    override fun receiveNewTransfer(receiverIOU: IOU) {
        this.hub.receiveNewTransfer(receiverIOU)
    }

    override fun queryNewTransfer(blockAddress: Address): OffchainTransaction? {
        return this.hub.queryNewTransfer(blockAddress)
    }

    override fun querySignedUpdate(blockAddress: Address): Update? {
        val hubAccount = this.hub.getHubAccount(blockAddress)
        return hubAccount?.update
    }

    override fun querySignedUpdate(eon: Int, blockAddress: Address): Update? {
        val hubAccount = this.hub.getHubAccount(eon, blockAddress)
        return hubAccount?.update
    }

    override fun getProof(blockAddress: Address): AMTreeProof? {
        return hub.getProof(blockAddress)
    }

    override fun getProof(eon: Int, blockAddress: Address): AMTreeProof? {
        return hub.getProof(eon, blockAddress)
    }

    override fun watch(address: Address): ReceiveChannel<HubEvent> {
        return hub.watch(address)
    }

    override fun watch(predicate: (HubEvent) -> Boolean): ReceiveChannel<HubEvent> {
        return hub.watch(predicate)
    }

    override fun getHubAccount(blockAddress: Address): HubAccount? {
        return this.hub.getHubAccount(blockAddress)
    }

    override fun resetHubMaliciousFlag(): EnumSet<HubMaliciousFlag> {
        return this.hub.resetHubMaliciousFlag()
    }
}
