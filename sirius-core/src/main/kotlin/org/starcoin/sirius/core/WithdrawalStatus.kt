package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema
import java.math.BigInteger

@ProtobufSchema(Starcoin.WithdrawalStatus::class)
@Serializable
data class WithdrawalStatus(
    @SerialId(1)
    var type: WithdrawalStatusType = WithdrawalStatusType.INIT,
    @SerialId(2)
    val withdrawal: Withdrawal = Withdrawal.DUMMY_WITHDRAWAL
) : SiriusObject() {

    @Transient
    val withdrawalAmount: BigInteger
        get() = this.withdrawal.amount

    @Transient
    val status: Int
        get() = this.type.number

    @Transient
    val isInit: Boolean
        get() = this.type == WithdrawalStatusType.INIT

    @Transient
    val eon: Int
        get() = this.withdrawal.proof.path.eon

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

