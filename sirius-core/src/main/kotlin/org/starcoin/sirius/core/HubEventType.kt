package org.starcoin.sirius.core

import org.apache.commons.lang3.RandomUtils
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

enum class HubEventType(
    private val payloadClass: KClass<*>
) {
    NEW_HUB_ROOT(HubRoot::class),
    NEW_DEPOSIT(Deposit::class),
    WITHDRAWAL(
        WithdrawalStatus::class
    ),
    NEW_TX(OffchainTransaction::class),
    NEW_UPDATE(Update::class);


    @Suppress("UNCHECKED_CAST")
    fun mock(): SiriusObject {
        return (payloadClass.companionObjectInstance as SiriusObjectCompanion<*, *>).mock()
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : SiriusObject> parsePayload(payloadBytes: ByteArray): D {
        return (payloadClass.companionObjectInstance as SiriusObjectCompanion<*, *>).parseFromProtobuf(payloadBytes) as D
    }

    companion object {

        fun valueOf(type: Int): HubEventType {
            return HubEventType.values()[type]
        }

        fun random(): HubEventType {
            return HubEventType.values()[RandomUtils.nextInt(0, HubEventType.values().size)]
        }
    }
}
