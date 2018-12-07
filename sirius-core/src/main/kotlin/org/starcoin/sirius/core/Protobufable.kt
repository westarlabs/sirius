package org.starcoin.sirius.core

import com.google.protobuf.GeneratedMessageV3

interface Protobufable<P : GeneratedMessageV3> {

    fun toProto(): P

}
