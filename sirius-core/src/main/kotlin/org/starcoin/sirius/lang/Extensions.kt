package org.starcoin.sirius.lang

import org.starcoin.sirius.util.Utils
import java.math.BigInteger

fun ByteArray.toULong() = BigInteger(1, this).toLong()

fun ByteArray.toHEXString() = Utils.HEX.encode(this)
