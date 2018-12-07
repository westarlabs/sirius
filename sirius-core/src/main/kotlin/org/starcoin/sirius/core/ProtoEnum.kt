package org.starcoin.sirius.core

import com.google.protobuf.ProtocolMessageEnum

interface ProtoEnum<T : ProtocolMessageEnum> {

    val number: Int
        get() = this.toProto().number

    fun toProto(): T

}
