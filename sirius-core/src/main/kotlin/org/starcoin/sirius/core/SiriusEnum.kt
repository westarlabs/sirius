package org.starcoin.sirius.core

import com.google.protobuf.ProtocolMessageEnum
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.util.MockUtils
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

interface SiriusEnum<T : ProtocolMessageEnum> {

    val number: Int
        get() = this.toProto().number

    fun toProto(): T

}

enum class PathDirection :
    SiriusEnum<Starcoin.PathDirection> {
    ROOT,
    LEFT,
    RIGHT;

    override fun toProto(): Starcoin.PathDirection {
        return Starcoin.PathDirection.forNumber(this.ordinal)
    }

    companion object {

        fun valueOf(number: Int): PathDirection {
            for (direction in PathDirection.values()) {
                if (direction.number == number) {
                    return direction
                }
            }
            return PathDirection.ROOT
        }

        fun random(): PathDirection {
            return PathDirection.values()[MockUtils.nextInt(
                0,
                values().size
            )]
        }
    }
}

enum class WithdrawalStatusType constructor(private val protoType: Starcoin.WithdrawalStatusType) :
    SiriusEnum<Starcoin.WithdrawalStatusType> {
    INIT(Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_INIT),
    CANCEL(Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL),
    PASSED(Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_PASSED),
    CONFIRMED(Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CONFIRMED),
    CLIENTCONFIRMED(Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED);

    override fun toProto(): Starcoin.WithdrawalStatusType {
        return protoType
    }

    companion object {

        fun valueOf(type: Int): WithdrawalStatusType {
            for (statusType in WithdrawalStatusType.values()) {
                if (statusType.number == type) {
                    return statusType
                }
            }
            throw IllegalArgumentException("Unsupported status type:$type")
        }
    }
}

enum class ChallengeStatus constructor(private val stat: Starcoin.ChallengeStatus) :
    SiriusEnum<Starcoin.ChallengeStatus> {
    OPEN(Starcoin.ChallengeStatus.OPEN), CLOSE(Starcoin.ChallengeStatus.CLOSE);

    override fun toProto(): Starcoin.ChallengeStatus {
        return stat
    }

    companion object {

        fun valueOf(stat: Int): ChallengeStatus {
            for (s in ChallengeStatus.values()) {
                if (s.number == stat) {
                    return s
                }
            }
            throw IllegalArgumentException("Unsupported status type:$stat")
        }
    }
}

enum class HubEventType(
    private val payloadClass: KClass<*>
) : SiriusEnum<Starcoin.HubEventType> {
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

    override fun toProto(): Starcoin.HubEventType {
        return Starcoin.HubEventType.forNumber(this.ordinal)
    }

    companion object {

        fun valueOf(type: Int): HubEventType {
            return HubEventType.values()[type]
        }

        fun random(): HubEventType {
            return HubEventType.values()[RandomUtils.nextInt(
                0,
                values().size
            )]
        }
    }
}
