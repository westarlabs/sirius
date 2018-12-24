package org.starcoin.sirius.serialization

import com.google.protobuf.GeneratedMessageV3
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class ProtobufSchema(val schema: KClass<out GeneratedMessageV3>)
