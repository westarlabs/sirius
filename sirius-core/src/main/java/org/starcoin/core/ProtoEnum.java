package org.starcoin.core;

import com.google.protobuf.ProtocolMessageEnum;

public interface ProtoEnum<T extends ProtocolMessageEnum> {

    T toProto();

    int getNumber();

}
