package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.ProtoWithdrawalStatus
import org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType

import java.util.Objects
import java.util.logging.Logger

/**
 * Created by dqm on 2018/10/4.
 */
class WithdrawalStatus : ProtobufCodec<ProtoWithdrawalStatus> {

    private val logger = Logger.getLogger(WithdrawalStatus::class.java.name)

    private var withdrawal: Withdrawal? = null
    private var type: WithdrawalStatusType? = null

    val isInit: Boolean
        get() = this.type == WithdrawalStatusType.INIT

    val isPassed: Boolean
        get() = this.type == WithdrawalStatusType.PASSED

    val eon: Int
        get() = this.withdrawal!!.path!!.eon

    val withdrawalAmount: Long
        get() = this.withdrawal!!.amount

    val address: BlockAddress?
        get() = this.withdrawal!!.address

    val status: Int
        get() = this.type!!.number

    constructor() {}

    enum class WithdrawalStatusType constructor(private val protoType: ProtoWithdrawalStatusType) :
        ProtoEnum<ProtoWithdrawalStatusType> {
        INIT(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_INIT),
        CANCEL(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL),
        PASSED(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_PASSED),
        CONFIRMED(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CONFIRMED),
        CLIENTCONFIRMED(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED);


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

    constructor(withdrawal: Withdrawal) {
        this.withdrawal = withdrawal
        this.type = WithdrawalStatusType.INIT
    }

    constructor(proto: ProtoWithdrawalStatus) {
        this.unmarshalProto(proto)
    }

    fun cancel(): Boolean {
        if (isInit) {
            this.type = WithdrawalStatusType.CANCEL
            logger.warning("cancel succ")
            return true
        }

        logger.warning("cancel fail : " + if (this.type == null) "null" else this.type!!.number)
        return false
    }

    fun pass(): Boolean {
        if (isInit) {
            this.type = WithdrawalStatusType.PASSED
            logger.warning("pass succ")
            return true
        }

        logger.warning("pass fail : " + if (this.type == null) "null" else this.type!!.number)
        return false
    }

    fun finish(): Boolean {
        return this.type == WithdrawalStatusType.CONFIRMED || this.type == WithdrawalStatusType.CANCEL
    }

    fun confirm(): Boolean {
        if (this.type == WithdrawalStatusType.PASSED) {
            this.type = WithdrawalStatusType.CONFIRMED
            logger.warning("confirm succ")
            return true
        }

        logger.warning("confirm fail : " + if (this.type == null) "null" else this.type!!.number)
        return false
    }

    override fun marshalProto(): ProtoWithdrawalStatus {
        return ProtoWithdrawalStatus.newBuilder()
            .setWithdrawal(this.withdrawal!!.marshalProto())
            .setType(type!!.toProto())
            .build()
    }

    override fun unmarshalProto(proto: ProtoWithdrawalStatus) {
        this.type = WithdrawalStatusType.valueOf(proto.type.number)
        this.withdrawal = if (proto.hasWithdrawal()) Withdrawal(proto.withdrawal) else null
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is WithdrawalStatus) {
            return false
        }
        val that = o as WithdrawalStatus?
        return withdrawal == that!!.withdrawal && type == that.type
    }

    override fun hashCode(): Int {
        return Objects.hash(withdrawal, type)
    }

    fun clientConfirm(): Boolean {
        if (this.type == WithdrawalStatusType.PASSED || isInit || this.type == WithdrawalStatusType.CONFIRMED) {
            this.type = WithdrawalStatusType.CLIENTCONFIRMED
            logger.warning("confirm succ")
            return true
        }

        logger.warning("client confirm fail : " + if (this.type == null) "null" else this.type?.number)
        return false
    }

}
