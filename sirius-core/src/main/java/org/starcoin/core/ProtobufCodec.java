package org.starcoin.core;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

public interface ProtobufCodec<P extends GeneratedMessageV3> extends Protobufable<P> {

    P marshalProto();

    void unmarshalProto(P proto);

    @Override
    default P toProto() {
        return this.marshalProto();
    }


    default String toJson() {
        try {
            return JsonFormat.printer().print(this.toProto());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

}
