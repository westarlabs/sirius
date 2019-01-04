package org.starcoin.sirius.core

import org.starcoin.sirius.crypto.CryptoService
import java.math.BigInteger

data class Receipt(
    // NOTE: Not implement logs
    val transactionHash: Hash, val transactionIndex: BigInteger,
    val blockHash: Hash, val blockNumber: BigInteger,
    val contractAddress: Address?, val from: Address, val to: Address,
    val gasUsed: BigInteger, val logBloom: String, val cumulativeGasUsed: BigInteger,
    val root: String, val status: Boolean
) {
    constructor(
        transactionHash: String, transactionIndex: BigInteger,
        blockHash: String, blockNumber: BigInteger,
        contractAddress: String?, from: String, to: String,
        gasUsed: BigInteger, logBloom: String, cumulativeGasUsed: BigInteger,
        root: String, status: Boolean
    ) : this(
        CryptoService.hash(transactionHash.toByteArray()), transactionIndex,
        CryptoService.hash(blockHash.toByteArray()), blockNumber,
        if (contractAddress!=null) Address.wrap(contractAddress!!) else null, Address.wrap(from), Address.wrap(to),
        gasUsed, logBloom, cumulativeGasUsed, root, status
    )

    constructor(
        transactionHash: ByteArray, transactionIndex: BigInteger,
        blockHash: ByteArray, blockNumber: BigInteger,
        contractAddress: String?, from: ByteArray, to: ByteArray,
        gasUsed: BigInteger, logBloom: String, cumulativeGasUsed: BigInteger,
        root: String, status: Boolean
    ) : this(
        CryptoService.hash(transactionHash), transactionIndex,
        CryptoService.hash(blockHash), blockNumber,
        if (contractAddress!=null) Address.wrap(contractAddress!!) else null, Address.wrap(from), Address.wrap(to),
        gasUsed, logBloom, cumulativeGasUsed, root, status
    )

}
