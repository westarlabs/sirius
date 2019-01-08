package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.protocol.ContractFunction

fun ContractFunction.signature(): ByteArray {
    return doSignature(this.name)
}

private fun doSignature(functionName: String): ByteArray {
    return Hash.of(formatSignature(functionName).toByteArray()).toBytes().copyOfRange(0, 4)
}

fun formatSignature(functionName: String): String {
    return "$functionName(bytes)"
}

fun ContractFunction.encode(input: SiriusObject): ByteArray {
    return this.signature() + input.toRLP()
}

fun ContractFunction.Companion.getFunction(signature: ByteArray): ContractFunction? {
    //TODO optimize
    return functions.find { it.signature().contentEquals(signature) }
}
