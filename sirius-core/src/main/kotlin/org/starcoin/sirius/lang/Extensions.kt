package org.starcoin.sirius.lang

import java.math.BigInteger

fun ByteArray.toULong() = BigInteger(1, this).toLong()
