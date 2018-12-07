package org.starcoin.sirius.core

import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.util.JsonFormat

interface ProtobufCodec<P : GeneratedMessageV3> : Protobufable<P> {

    fun marshalProto(): P

    fun unmarshalProto(proto: P)

    override fun toProto(): P {
        return this.marshalProto()
    }


    fun toJson(): String {
        try {
            return JsonFormat.printer().print(this.toProto())
        } catch (e: InvalidProtocolBufferException) {
            throw RuntimeException(e)
        }

    }

}
