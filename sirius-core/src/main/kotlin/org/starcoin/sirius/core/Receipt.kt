package org.starcoin.sirius.core

import org.starcoin.sirius.crypto.CryptoService
import java.math.BigInteger

data class Receipt(
    val transactionHash: Hash, val transactionIndex: BigInteger,
    val blockHash: Hash, val blockNumber: BigInteger,
    val contractAddress: Address?, val from: Address, val to: Address?,
    val gasUsed: BigInteger, val logBloom: String, val cumulativeGasUsed: BigInteger,
    val root: String?, val status: Boolean, val logs: List<String>?
) {
    constructor(
        transactionHash: String, transactionIndex: BigInteger,
        blockHash: String, blockNumber: BigInteger,
        contractAddress: String?, from: String, to: String?,
        gasUsed: BigInteger, logBloom: String, cumulativeGasUsed: BigInteger,
        root: String?, status: Boolean, logs: List<String>?
    ) : this(
        CryptoService.hash(transactionHash.toByteArray()), transactionIndex,
        CryptoService.hash(blockHash.toByteArray()), blockNumber,
        contractAddress?.let { Address.wrap(contractAddress) }, Address.wrap(from),
        if (to == null || to.isEmpty()) null else Address.wrap(to),
        gasUsed, logBloom, cumulativeGasUsed, root, status, logs
    )

    constructor(
        transactionHash: ByteArray, transactionIndex: BigInteger,
        blockHash: ByteArray, blockNumber: BigInteger,
        contractAddress: String?, from: ByteArray, to: ByteArray?,
        gasUsed: BigInteger, logBloom: String, cumulativeGasUsed: BigInteger,
        root: String?, status: Boolean, logs: List<String>?
    ) : this(
        CryptoService.hash(transactionHash), transactionIndex,
        CryptoService.hash(blockHash), blockNumber,
        if (contractAddress != null) Address.wrap(contractAddress!!) else null,
        Address.wrap(from),
        if (to == null || to.isEmpty()) null else Address.wrap(to),
        gasUsed, logBloom, cumulativeGasUsed, root, status, logs
    )
}
