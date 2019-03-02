package org.starcoin.sirius.serialization

interface Codec<T> {

    fun encode(value: T): ByteArray

    fun decode(bytes: ByteArray): T
}