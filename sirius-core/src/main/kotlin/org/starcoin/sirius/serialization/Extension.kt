package org.starcoin.sirius.serialization

import com.google.protobuf.ByteString
import java.math.BigInteger


fun ByteArray.toByteString() = ByteString.copyFrom(this)!!
fun BigInteger.toByteArrayRemoveLeadZero() =
    this.toByteArray().let { if (it[0] == 0.toByte()) it.drop(1).toByteArray() else it }!!

fun ByteArray.stripHexPrefix():ByteArray {
    return if (copyOfRange(0, 2).contentEquals("0x".toByteArray()))
        copyOfRange(2, size)
    else this
}