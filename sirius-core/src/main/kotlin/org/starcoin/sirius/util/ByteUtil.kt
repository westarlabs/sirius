package org.starcoin.sirius.util

import java.math.BigInteger

object ByteUtil {

    /**
     * copy from ethereumj
     *
     * The regular [java.math.BigInteger.toByteArray] method isn't quite what we often need:
     * it appends a leading zero to indicate that the number is positive and may need padding.
     *
     * @param b the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    fun bigIntegerToBytes(b: BigInteger, numBytes: Int): ByteArray {
        val bytes = ByteArray(numBytes)
        val biBytes = b.toByteArray()
        val start = if (biBytes.size == numBytes + 1) 1 else 0
        val length = Math.min(biBytes.size, numBytes)
        System.arraycopy(biBytes, start, bytes, numBytes - length, length)
        return bytes
    }
}
