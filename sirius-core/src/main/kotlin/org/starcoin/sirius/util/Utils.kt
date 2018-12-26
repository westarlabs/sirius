package org.starcoin.sirius.util

import com.google.common.io.BaseEncoding
import java.util.*

object Utils {

    val HEX = BaseEncoding.base16().lowerCase()

    fun timestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun newNonce(): Long {
        return MockUtils.nextLong()
    }

    fun newReuqestID(): String {
        // TODO
        return UUID.randomUUID().toString()
    }
}
