package org.starcoin.sirius.core

import com.google.protobuf.GeneratedMessageV3
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.context.getOrDefault
import kotlinx.serialization.json.JSON
import kotlinx.serialization.load
import kotlinx.serialization.parse
import org.starcoin.sirius.lang.resetableLazy
import org.starcoin.sirius.serialization.Codec
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.serialization.protobuf.ProtoBuf
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.WithLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.staticFunctions

//@Serializable
abstract class SiriusObject : Hashable {

    @kotlinx.serialization.Transient
    private val hashDelegate = resetableLazy { doHash() }

    @kotlinx.serialization.Transient
    val id: Hash by hashDelegate

    override fun hash(): Hash {
        return id
    }

    protected open fun doHash(): Hash {
        return Hash.of(this)
    }

    //Subclasses should call this fun when property change.
    protected fun resetHash() {
        hashDelegate.reset()
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toRLP(): ByteArray {
        return RLP.dump(RLP.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toProtobuf(): ByteArray {
        return ProtoBuf.dump(ProtoBuf.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    @Suppress("UNCHECKED_CAST")
    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toJSON(): String {
        return JSON.stringify(JSON.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    override fun toString(): String {
        return toJSON()
    }

    fun <P : GeneratedMessageV3> toProto(): P {
        val companion = this::class.companionObjectInstance as SiriusObjectCompanion<SiriusObject, P>
        return companion.toProtoMessage(this)
    }

    companion object {
        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> parseFromRLP(bytes: ByteArray): T {
            return RLP.load(bytes)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        fun <T : SiriusObject> parseFromRLP(clazz: KClass<T>, bytes: ByteArray): T {
            return RLP.load(RLP.plain.context.getOrDefault(clazz), bytes)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> parseFromJSON(json: String): T {
            return JSON.parse(json)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> parseFromProtobuf(bytes: ByteArray): T {
            return ProtoBuf.load(bytes)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        fun <T : SiriusObject> parseFromProtobuf(clazz: KClass<T>, bytes: ByteArray): T {
            return ProtoBuf.load(ProtoBuf.plain.context.getOrDefault(clazz), bytes)
        }
    }
}

abstract class SiriusObjectCompanion<T : SiriusObject, P : GeneratedMessageV3>(val objClass: KClass<T>) :
    WithLogging() {

    //TODO write a auto mock implements.
    abstract fun mock(): T

    open fun parseFromProtoMessage(protoMessage: P): T {
        return parseFromProtobuf(protoMessage.toByteArray())
    }

    @Suppress("UNCHECKED_CAST")
    open fun toProtoMessage(obj: T): P {
        //TODO custom exception
        val protobufSchema =
            obj::class.annotations.find { it.annotationClass == ProtobufSchema::class } as? ProtobufSchema
                ?: throw RuntimeException("Can not auto Proto type convert, please use ${ProtobufSchema::class.qualifiedName} annotation")
        val protoClass = protobufSchema.schema
        return protoClass.staticFunctions.find {
            it.name == "parseFrom" && it.parameters.size == 1 && it.parameters[0].type.classifier == ByteArray::class
        }?.call(
            obj.toProtobuf()
        ) as? P
            ?: throw throw RuntimeException("Can not find parseFrom method from ${protoClass.qualifiedName}")
    }

    open fun toRLP(obj: T): ByteArray {
        return obj.toRLP()
    }

    open fun toJSON(obj: T): String {
        return obj.toJSON()
    }

    open fun toProtobuf(obj: T): ByteArray {
        return obj.toProtobuf()
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    open fun parseFromRLP(bytes: ByteArray): T {
        return RLP.load(RLP.plain.context.getOrDefault(objClass), bytes)
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    open fun parseFromJSON(json: String): T {
        return JSON.parse(RLP.plain.context.getOrDefault(objClass), json)
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    open fun parseFromProtobuf(bytes: ByteArray): T {
        return ProtoBuf.load(ProtoBuf.plain.context.getOrDefault(objClass), bytes)
    }
}

inline fun <reified P : GeneratedMessageV3, reified T : SiriusObject> P.toSiriusObject(): T {
    val companion = T::class.companionObjectInstance as SiriusObjectCompanion<T, P>
    return companion.parseFromProtoMessage(this)
}

open abstract class SiriusObjectCodec<T : SiriusObject>(protected val clazz: KClass<T>) : Codec<T>

class SiriusObjectProfoBufCodec<T : SiriusObject>(clazz: KClass<T>) : SiriusObjectCodec<T>(clazz) {

    override fun encode(value: T): ByteArray {
        return value.toProtobuf()
    }

    override fun decode(bytes: ByteArray): T {
        return SiriusObject.parseFromProtobuf(clazz, bytes)
    }

}

class SiriusObjectRLPCodec<T : SiriusObject>(clazz: KClass<T>) : SiriusObjectCodec<T>(clazz) {

    override fun encode(value: T): ByteArray {
        return value.toRLP()
    }

    override fun decode(bytes: ByteArray): T {
        return SiriusObject.parseFromRLP(clazz, bytes)
    }

}