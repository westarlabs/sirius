package org.starcoin.sirius.serialization.rlp

import java.util.*

/**
 * original from https://github.com/walleth/kethereum
RLP as of Appendix B. Recursive Length Prefix at https://github.com/ethereum/yellowpaper
 */


/**
 * Reason for threshold according to Vitalik Buterin:
 * - 56 bytes maximizes the benefit of both options
 * - if we went with 60 then we would have only had 4 slots for long strings
 * so RLP would not have been able to store objects above 4gb
 * - if we went with 48 then RLP would be fine for 2^128 space, but that's way too much
 * - so 56 and 2^64 space seems like the right place to put the cutoff
 * - also, that's where Bitcoin's varint does the cutof
 */
internal const val SIZE_THRESHOLD = 56

/** RLP encoding rules are defined as follows: */

/*
     * For a single byte whose value is in the [0x00, 0x7f] range, that byte is
     * its own RLP encoding.
     */

/**
 * [0x80]
 * If a string is 0-55 bytes long, the RLP encoding consists of a single
 * byte with value 0x80 plus the length of the string followed by the
 * string. The range of the first byte is thus [0x80, 0xb7].
 */
internal const val OFFSET_SHORT_ITEM = 0x80

internal const val ELEMENT_OFFSET = OFFSET_SHORT_ITEM
/**
 * [0xb7]
 * If a string is more than 55 bytes long, the RLP encoding consists of a
 * single byte with value 0xb7 plus the length of the length of the string
 * in binary form, followed by the length of the string, followed by the
 * string. For example, a length-1024 string would be encoded as
 * \xb9\x04\x00 followed by the string. The range of the first byte is thus
 * [0xb8, 0xbf].
 */
internal const val OFFSET_LONG_ITEM = 0xb7

/**
 * [0xc0]
 * If the total payload of a list (i.e. the combined length of all its
 * items) is 0-55 bytes long, the RLP encoding consists of a single byte
 * with value 0xc0 plus the length of the list followed by the concatenation
 * of the RLP encodings of the items. The range of the first byte is thus
 * [0xc0, 0xf7].
 */
internal const val OFFSET_SHORT_LIST = 0xc0
internal const val LIST_OFFSET = OFFSET_SHORT_LIST
/**
 * [0xf7]
 * If the total payload of a list is more than 55 bytes long, the RLP
 * encoding consists of a single byte with value 0xf7 plus the length of the
 * length of the list in binary form, followed by the length of the list,
 * followed by the concatenation of the RLP encodings of the items. The
 * range of the first byte is thus [0xf8, 0xff].
 */
internal const val OFFSET_LONG_LIST = 0xf7

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

internal val EMPTY_LIST = RLPList()
internal val EMPTY_ELEMENT = RLPElement(ByteArray(0))
