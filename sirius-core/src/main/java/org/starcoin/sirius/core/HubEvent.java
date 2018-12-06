package org.starcoin.sirius.core;

import com.google.protobuf.Any;
import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoHubEvent;

import java.util.Objects;

public class HubEvent<D extends ProtobufCodec> implements ProtobufCodec<ProtoHubEvent> {

    private HubEventType type;
    private BlockAddress address;
    private D payload;

    public HubEvent() {
    }

    public HubEvent(ProtoHubEvent proto) {
        this.unmarshalProto(proto);
    }

    public HubEvent(HubEventType type, BlockAddress address, D payload) {
        this.type = type;
        this.address = address;
        this.payload = payload;
    }

    public HubEvent(HubEventType type, D payload) {
        this.type = type;
        this.payload = payload;
    }

    public boolean isPublicEvent() {
        return this.address == null;
    }

    public HubEventType getType() {
        return type;
    }

    public BlockAddress getAddress() {
        return address;
    }

    public D getPayload() {
        return payload;
    }

    @Override
    public ProtoHubEvent marshalProto() {
        ProtoHubEvent.Builder builder = ProtoHubEvent.newBuilder().setType(this.type.toProto());
        if (this.address != null) {
            builder.setAddress(address.toProto());
        }
        if (this.payload != null) {
            builder.setPayload(Any.pack(payload.toProto()));
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(ProtoHubEvent proto) {
        this.type = HubEventType.valueOf(proto.getTypeValue());
        this.address = proto.hasAddress() ? BlockAddress.valueOf(proto.getAddress()) : null;
        this.payload = proto.hasPayload() ? this.type.parsePayload(proto.getPayload()) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HubEvent)) {
            return false;
        }
        HubEvent<?> hubEvent = (HubEvent<?>) o;
        return type == hubEvent.type
                && Objects.equals(address, hubEvent.address)
                && Objects.equals(payload, hubEvent.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, address, payload);
    }

    public String toString() {
        // TODO use toJson.
        return this.type.name()
                + " "
                + (this.address == null ? "" : this.address.toString())
                + " "
                + this.payload.toJson();
    }
}
