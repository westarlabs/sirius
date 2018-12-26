package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.InitiateWithdrawalRequest::class)
@Serializable
data class WithdrawalStatus(
    @SerialId(1)
    var withdrawal: Withdrawal = Withdrawal.DUMMY_WITHDRAWAL,
    @SerialId(2)
    var type: WithdrawalStatusType = WithdrawalStatusType.INIT
) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<WithdrawalStatus, Starcoin.ProtoWithdrawalStatus>(WithdrawalStatus::class) {

        var DUMMY_WITHDRAWAL_STATUS = WithdrawalStatus()

        override fun mock(): WithdrawalStatus {
            return WithdrawalStatus(Withdrawal.mock(), WithdrawalStatusType.INIT)
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