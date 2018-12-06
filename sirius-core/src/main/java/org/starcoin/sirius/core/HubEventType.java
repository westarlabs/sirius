package org.starcoin.sirius.core;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.starcoin.core.ProtoEnum;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.*;

import java.lang.reflect.InvocationTargetException;

public enum HubEventType implements ProtoEnum<ProtoHubEventType> {
    NEW_HUB_ROOT(ProtoHubEventType.HUB_EVENT_NEW_HUB_ROOT, HubRoot.class, ProtoHubRoot.class),
    NEW_DEPOSIT(ProtoHubEventType.HUB_EVENT_NEW_DEPOSIT, Deposit.class, DepositRequest.class),
    WITHDRAWAL(
            ProtoHubEventType.HUB_EVENT_WITHDRAWAL, WithdrawalStatus.class, ProtoWithdrawalStatus.class),
    NEW_TX(
            ProtoHubEventType.HUB_EVENT_NEW_TX,
            OffchainTransaction.class,
            ProtoOffchainTransaction.class),
    NEW_UPDATE(ProtoHubEventType.HUB_EVENT_NEW_UPDATE, Update.class, ProtoUpdate.class);

    private ProtoHubEventType protoHubEventType;
    private Class payloadClass;
    private Class protoPayloadClass;

    HubEventType(ProtoHubEventType protoHubEventType, Class payloadClass, Class protoPayloadClass) {
        this.protoHubEventType = protoHubEventType;
        this.payloadClass = payloadClass;
        this.protoPayloadClass = protoPayloadClass;
    }

    @Override
    public ProtoHubEventType toProto() {
        return this.protoHubEventType;
    }

    @Override
    public int getNumber() {
        return this.protoHubEventType.getNumber();
    }

    public <D extends ProtobufCodec> D parsePayload(Any any) {
        try {
            if (this.payloadClass == null || this.protoPayloadClass == null) {
                return null;
            }
            com.google.protobuf.Message protoMessage = any.unpack(this.protoPayloadClass);
            return (D) this.payloadClass.getConstructor(this.protoPayloadClass).newInstance(protoMessage);
        } catch (InvalidProtocolBufferException
                | NoSuchMethodException
                | IllegalAccessException
                | InstantiationException
                | InvocationTargetException e) {
            // TODO exception
            throw new RuntimeException(e);
        }
    }

    public static HubEventType valueOf(int type) {
        for (HubEventType eventType : HubEventType.values()) {
            if (eventType.getNumber() == type) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Unsupported event type:" + type);
    }
}
