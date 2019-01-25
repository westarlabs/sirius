package org.starcoin.sirius.serialization.protobuf

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialId
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerializationException

enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoType(val type: ProtoNumberType)

typealias ProtoDesc = Pair<Int, ProtoNumberType>

fun extractParameters(desc: SerialDescriptor, index: Int): ProtoDesc {
    val tag = desc.getElementAnnotations(index).filterIsInstance<SerialId>().onlySingleOrNull()?.id ?: index
    val format = desc.getElementAnnotations(index).filterIsInstance<ProtoType>().onlySingleOrNull()?.type
        ?: ProtoNumberType.DEFAULT
    return tag to format
}


class ProtobufDecodingException(message: String) : SerializationException(message)
