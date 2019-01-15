package org.starcoin.sirius.chain

import org.starcoin.sirius.chain.fallback.DefaultChainStrategy
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.ContractFunction
import org.starcoin.sirius.protocol.FunctionSignature
import java.math.BigInteger
import java.util.*
import kotlin.reflect.KClass

interface ChainStrategy {

    fun <S : SiriusObject> encode(obj: S): ByteArray

    fun <S : SiriusObject> decode(bytes: ByteArray, clazz: KClass<S>): S

    fun signature(function: ContractFunction<*>): FunctionSignature

    fun <S : SiriusObject> encode(function: ContractFunction<S>, input: S): ByteArray

    fun <S : SiriusObject> decode(function: ContractFunction<S>, bytes: ByteArray): S

    companion object : ChainStrategy {
        val instance: ChainStrategy by lazy {
            val loaders = ServiceLoader
                .load(ChainStrategyProvider::class.java).iterator()
            if (loaders.hasNext()) {
                loaders.next().createChainStrategy()
            } else {
                //if can not find, use fallback
                DefaultChainStrategy
            }
        }

        override fun <S : SiriusObject> encode(obj: S) = instance.encode(obj)

        override fun <S : SiriusObject> decode(bytes: ByteArray, clazz: KClass<S>) = instance.decode(bytes, clazz)

        override fun signature(function: ContractFunction<*>) = instance.signature(function)

        override fun <S : SiriusObject> encode(function: ContractFunction<S>, input: S) =
            instance.encode(function, input)

        override fun <S : SiriusObject> decode(function: ContractFunction<S>, bytes: ByteArray) =
            instance.decode(function, bytes)
    }
}
