package org.starcoin.core;

import com.google.protobuf.GeneratedMessageV3;

public interface Protobufable<P extends GeneratedMessageV3> {

    P toProto();

}
