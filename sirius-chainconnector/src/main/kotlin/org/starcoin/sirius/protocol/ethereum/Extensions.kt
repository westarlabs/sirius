package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.CallTransaction
import org.ethereum.db.ByteArrayWrapper
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.protocol.ContractFunction

fun ContractFunction<*>.signature(): ByteArray {
    return doSignature(this.name)
}

private fun doSignature(functionName: String): ByteArray {
    return Hash.of(formatSignature(functionName).toByteArray()).toBytes().copyOfRange(0, 4)
}

fun formatSignature(functionName: String): String {
    return "$functionName(bytes)"
}

fun ContractFunction<*>.encode(input: SiriusObject): ByteArray {
    val function = CallTransaction.Function.fromSignature(this.name, arrayOf("bytes"), arrayOf("bool"))
    return function.encode(input.toRLP())
}

val ContractFunction.Companion.functionMap: Map<ByteArrayWrapper, ContractFunction<*>>
    get() = functions.map { ByteArrayWrapper(it.signature()) to it }.toMap()

