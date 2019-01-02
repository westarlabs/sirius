package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.WithdrawalStatus::class)
@Serializable
data class WithdrawalStatus(
    @SerialId(1)
    var type: WithdrawalStatusType = WithdrawalStatusType.INIT,
    @SerialId(2)
    val withdrawal: Withdrawal = Withdrawal.DUMMY_WITHDRAWAL
) : SiriusObject() {

    @Transient
    val withdrawalAmount: Long
        get() = this.withdrawal.amount

    @Transient
    val status: Int
        get() = this.type.number

    @Transient
    val isInit: Boolean
        get() = this.type == WithdrawalStatusType.INIT

    @Transient
    val eon: Int
        get() = this.withdrawal.path.eon

    fun pass() {
        if (isInit) {
            this.type = WithdrawalStatusType.PASSED
        }
    }

    fun cancel() {
        if (isInit) {
            this.type = WithdrawalStatusType.CANCEL
        }
    }

    fun clientConfirm() {
        if (this.type == WithdrawalStatusType.PASSED || isInit || this.type == WithdrawalStatusType.CONFIRMED) {
            this.type = WithdrawalStatusType.CLIENTCONFIRMED
        }
    }

    companion object :
        SiriusObjectCompanion<WithdrawalStatus, Starcoin.WithdrawalStatus>(WithdrawalStatus::class) {

        var DUMMY_WITHDRAWAL_STATUS = WithdrawalStatus()

        override fun mock(): WithdrawalStatus {
            return WithdrawalStatus(WithdrawalStatusType.INIT, Withdrawal.mock())
        }
    }
}

enum class WithdrawalStatusType constructor(private val protoType: ProtoWithdrawalStatusType) :
    ProtoEnum<Starcoin.ProtoWithdrawalStatusType> {
    INIT(Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_INIT),
    CANCEL(Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL),
    PASSED(Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_PASSED),
    CONFIRMED(Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CONFIRMED),
    CLIENTCONFIRMED(Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED);

    override fun toProto(): ProtoWithdrawalStatusType {
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
