package org.starcoin.sirius.core

import kotlinx.serialization.*
import kotlinx.serialization.context.getOrDefault
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import org.starcoin.sirius.serialization.rlp.RLP

@Serializable
abstract class SiriusObject : Hashable {

    @Transient
    val id: Hash by lazy { Hash.of(this) }

    override fun hash(): Hash {
        return id
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toRLP(): ByteArray {
        return RLP.dump(RLP.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toProtobuf(): ByteArray {
        return ProtoBuf.dump(ProtoBuf.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun toJSON(): String {
        return JSON.stringify(JSON.plain.context.getOrDefault(this::class) as KSerializer<SiriusObject>, this)
    }

    companion object {
        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> fromRLP(bytes: ByteArray): T {
            return RLP.load(bytes)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> fromJSON(json: String): T {
            return JSON.parse(json)
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        inline fun <reified T : SiriusObject> fromProtobuf(bytes: ByteArray): T {
            return ProtoBuf.load(bytes)
        }
    }
}
