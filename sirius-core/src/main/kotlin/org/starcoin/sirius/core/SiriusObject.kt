package org.starcoin.sirius.core

import com.google.protobuf.GeneratedMessageV3
import kotlinx.serialization.KSerializer

abstract class SiriusObject<P : GeneratedMessageV3> : ProtobufCodec<P>, CachedHash() {

    override fun hashData(): ByteArray {
        return this.marshalProto().toByteArray()
    }

}
