package org.starcoin.sirius.serialization.rlp

import java.util.*

/**
 * original from https://github.com/walleth/kethereum
RLP as of Appendix B. Recursive Length Prefix at https://github.com/ethereum/yellowpaper
 */

internal const val ELEMENT_OFFSET = 128
internal const val LIST_OFFSET = 192

sealed class RLPType

data class RLPElement(val bytes: ByteArray) : RLPType() {

    override fun equals(other: Any?) = when (other) {
        is RLPElement -> Arrays.equals(bytes, other.bytes)
        else -> false
    }

    override fun hashCode() = Arrays.hashCode(bytes)
}

data class RLPList(val element: MutableList<RLPType>) : RLPType(), MutableList<RLPType> by element {
    constructor() : this(mutableListOf())
}

class IllegalRLPException(msg: String) : IllegalArgumentException(msg)