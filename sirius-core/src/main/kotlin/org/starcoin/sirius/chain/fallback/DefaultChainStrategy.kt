package org.starcoin.sirius.chain.fallback

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import org.starcoin.sirius.chain.ChainStrategy
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.protocol.ContractFunction
import org.starcoin.sirius.protocol.FunctionSignature
import org.starcoin.sirius.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.reflect.KClass

class DefaultFunctionSignature(val function: ContractFunction<*>) : FunctionSignature(function.name.toByteArray())


object DefaultChainStrategy : ChainStrategy {
    override fun <S : SiriusObject> encode(obj: S): ByteArray {
        return obj.toProtobuf()
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    override fun <S : SiriusObject> decode(bytes: ByteArray, clazz: KClass<S>): S {
        return ProtoBuf.load(clazz.serializer(), bytes)
    }

    override fun signature(function: ContractFunction<*>): FunctionSignature {
        return DefaultFunctionSignature(function)
    }

    override fun <S : SiriusObject> encode(function: ContractFunction<S>, input: S): ByteArray {
        val byteArrayOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteArrayOut)
        dataOut.writeInt(function.signature.value.size)
        dataOut.write(function.signature.value)
        dataOut.write(this.encode(input))
        return byteArrayOut.toByteArray()
    }

    override fun <S : SiriusObject> decode(function: ContractFunction<S>, bytes: ByteArray): S {
        val dataIn = DataInputStream(ByteArrayInputStream(bytes))
        val length = dataIn.readInt()
        assert(length > 0)
        val signature = ByteArray(length)
        dataIn.read(signature)
        val data = dataIn.readBytes()
        return this.decode(data, function.inputClass)
    }
}
