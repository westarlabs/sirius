package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.ProtoHubEvent::class)
@Serializable
data class HubEvent(
    @SerialId(1)
    val type: HubEventType,
    @SerialId(2)
    val payloadBytes: ByteArrayWrapper,
    @SerialId(3)
    val address: Address = Address.ZERO_ADDRESS
) : SiriusObject() {

    constructor(type: HubEventType, obj: SiriusObject, address: Address = Address.ZERO_ADDRESS) : this(
        type,
        ByteArrayWrapper(obj.toProtobuf()),
        address
    )


    fun <D : SiriusObject> getPayload(): D {
        return type.parsePayload(payloadBytes.bytes)
    }

    @Transient
    val isPublicEvent: Boolean
        get() = this.address != Address.ZERO_ADDRESS


    companion object : SiriusObjectCompanion<HubEvent, Starcoin.ProtoHubEvent>(HubEvent::class) {
        override fun mock(): HubEvent {
            val type = HubEventType.random()
            return when (type) {
                HubEventType.NEW_HUB_ROOT -> HubEvent(type, HubRoot.mock())
                HubEventType.NEW_DEPOSIT -> {
                    val obj = Deposit.mock()
                    HubEvent(type, obj, obj.address)
                }
                HubEventType.WITHDRAWAL -> {
                    val obj = Withdrawal.mock()
                    HubEvent(type, obj, obj.address)
                }
                HubEventType.NEW_TX -> {
                    val obj = OffchainTransaction.mock()
                    HubEvent(type, obj, obj.to)
                }
                HubEventType.NEW_UPDATE -> {
                    val obj = Update.mock()
                    HubEvent(type, obj, CryptoService.generateCryptoKey().address)
                }
            }
        }
    }
}
