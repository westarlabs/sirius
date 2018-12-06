package org.starcoin.sirius.core;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtoEnum;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoWithdrawalStatus;
import org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Created by dqm on 2018/10/4.
 */
public class WithdrawalStatus implements ProtobufCodec<ProtoWithdrawalStatus> {

    private Logger logger = Logger.getLogger(WithdrawalStatus.class.getName());

    public WithdrawalStatus() {
    }

    public enum WithdrawalStatusType implements ProtoEnum<ProtoWithdrawalStatusType> {
        INIT(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_INIT),
        CANCEL(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL),
        PASSED(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_PASSED),
        CONFIRMED(ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CONFIRMED);

        private ProtoWithdrawalStatusType protoType;

        WithdrawalStatusType(ProtoWithdrawalStatusType protoType) {
            this.protoType = protoType;
        }

        @Override
        public ProtoWithdrawalStatusType toProto() {
            return protoType;
        }

        @Override
        public int getNumber() {
            return protoType.getNumber();
        }

        public static WithdrawalStatusType valueOf(int type) {
            for (WithdrawalStatusType statusType : WithdrawalStatusType.values()) {
                if (statusType.getNumber() == type) {
                    return statusType;
                }
            }
            throw new IllegalArgumentException("Unsupported status type:" + type);
        }
    }

    private Withdrawal withdrawal;
    private WithdrawalStatusType type;

    public WithdrawalStatus(Withdrawal withdrawal) {
        this.withdrawal = withdrawal;
        this.type = WithdrawalStatusType.INIT;
    }

    public WithdrawalStatus(ProtoWithdrawalStatus proto) {
        this.unmarshalProto(proto);
    }

    public boolean isInit() {
        return (this.type == WithdrawalStatusType.INIT);
    }

    public boolean cancel() {
        if (isInit()) {
            this.type = WithdrawalStatusType.CANCEL;
            logger.warning("cancel succ");
            return true;
        }

        logger.warning("cancel fail : " + ((this.type == null) ? "null" : this.type.getNumber()));
        return false;
    }

    public boolean pass() {
        if (isInit()) {
            this.type = WithdrawalStatusType.PASSED;
            logger.warning("pass succ");
            return true;
        }

        logger.warning("pass fail : " + ((this.type == null) ? "null" : this.type.getNumber()));
        return false;
    }

    public boolean finish() {
        return (this.type == WithdrawalStatusType.CONFIRMED
                || (this.type == WithdrawalStatusType.CANCEL));
    }

    public boolean isPassed() {
        return (this.type == WithdrawalStatusType.PASSED);
    }

    public boolean confirm() {
        if (this.type == WithdrawalStatusType.PASSED) {
            this.type = WithdrawalStatusType.CONFIRMED;
            logger.warning("confirm succ");
            return true;
        }

        logger.warning("confirm fail : " + ((this.type == null) ? "null" : this.type.getNumber()));
        return false;
    }

    public int getEon() {
        return this.withdrawal.getPath().getEon();
    }

    public long getWithdrawalAmount() {
        return this.withdrawal.getAmount();
    }

    public BlockAddress getAddress() {
        return this.withdrawal.getAddress();
    }

    @Override
    public ProtoWithdrawalStatus marshalProto() {
        return ProtoWithdrawalStatus.newBuilder()
                .setWithdrawal(this.withdrawal.marshalProto())
                .setType(type.toProto())
                .build();
    }

    @Override
    public void unmarshalProto(ProtoWithdrawalStatus proto) {
        this.type = WithdrawalStatusType.valueOf(proto.getTypeValue());
        this.withdrawal = proto.hasWithdrawal() ? new Withdrawal(proto.getWithdrawal()) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WithdrawalStatus)) {
            return false;
        }
        WithdrawalStatus that = (WithdrawalStatus) o;
        return Objects.equals(withdrawal, that.withdrawal) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(withdrawal, type);
    }

    public int getStatus() {
        return this.type.getNumber();
    }
}
