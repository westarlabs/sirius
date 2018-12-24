package org.starcoin.sirius.core

import com.google.protobuf.Any
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.*

import java.lang.reflect.InvocationTargetException

enum class HubEventType(
    private val protoHubEventType: ProtoHubEventType,
    private val payloadClass: Class<*>?,
    private val protoPayloadClass: Class<*>?
) : ProtoEnum<ProtoHubEventType> {
    NEW_HUB_ROOT(ProtoHubEventType.HUB_EVENT_NEW_HUB_ROOT, HubRoot::class.java, ProtoHubRoot::class.java),
    NEW_DEPOSIT(ProtoHubEventType.HUB_EVENT_NEW_DEPOSIT, Deposit::class.java, DepositRequest::class.java),
    WITHDRAWAL(
        ProtoHubEventType.HUB_EVENT_WITHDRAWAL, WithdrawalStatus::class.java, ProtoWithdrawalStatus::class.java
    ),
    NEW_TX(
        ProtoHubEventType.HUB_EVENT_NEW_TX,
        OffchainTransaction::class.java,
        ProtoOffchainTransaction::class.java
    ),
    NEW_UPDATE(ProtoHubEventType.HUB_EVENT_NEW_UPDATE, UpdateData::class.java, Starcoin.UpdateData::class.java);

    override fun toProto(): ProtoHubEventType {
        return this.protoHubEventType
    }

    override val number: Int
        get() = this.protoHubEventType.number


    fun <D : SiriusObject> parsePayload(any: Any): D? {
        try {
            if (this.payloadClass == null || this.protoPayloadClass == null) {
                return null
            }
            val protoMessage = any.unpack<Message>(this.protoPayloadClass as Class<Message>?)
            return this.payloadClass.getConstructor(this.protoPayloadClass).newInstance(protoMessage) as D
        } catch (e: InvalidProtocolBufferException) {
            // TODO exception
            throw RuntimeException(e)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }

    }

    companion object {

        fun valueOf(type: Int): HubEventType {
            for (eventType in HubEventType.values()) {
                if (eventType.number == type) {
                    return eventType
                }
            }
            throw IllegalArgumentException("Unsupported event type:$type")
        }
    }
}
