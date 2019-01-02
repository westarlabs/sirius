package org.starcoin.sirius.core

import java.math.BigInteger

data class Receipt(
    // NOTE: Not implement logs
    val transactionHash: String, val transactionIndex: BigInteger,
    val blockHash: String, val blockNumber: BigInteger,
    val contractAddress: String, val from: String, val to: String,
    val gasUsed: BigInteger, val logBloom: String, val cumulativeGasUsed: BigInteger,
    val root: String, val status: Boolean
) {

}

