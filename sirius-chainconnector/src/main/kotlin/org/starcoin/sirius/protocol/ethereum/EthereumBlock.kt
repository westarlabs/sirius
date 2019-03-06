package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.TransactionReceipt
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.util.WithLogging
import org.web3j.protocol.core.methods.response.EthBlock

class EthereumBlock private constructor(override val height: Long, private val hash: Hash) :
    Block<EthereumTransaction>() {

    override lateinit var transactions: List<TransactionResult<EthereumTransaction>>

    constructor(ethBlock: EthBlock.Block, receipts: List<Receipt>) : this(
        ethBlock.number.longValueExact(),
        ethBlock.hash.toHash()
    ) {
        require(ethBlock.transactions.size == receipts.size)
        transactions =
            ethBlock.transactions.mapIndexed { index, it ->
                TransactionResult(
                    EthereumTransaction(it.get() as org.web3j.protocol.core.methods.response.Transaction),
                    receipts[index]
                )
            }
    }

    constructor(block: org.ethereum.core.Block, receipts: List<TransactionReceipt>) : this(
        block.number,
        block.hash.toHash()
    ) {
        LOG.info("org.ethereum.core.BlockSummary to EthereumBlock hash:${block.hash.toHash()}, height:${block.number}")
        this.transactions = block.transactionsList.mapIndexed { index, it ->
            TransactionResult(EthereumTransaction(it), EthereumReceipt(receipts[index]))
        }
    }

    override fun blockHash(): Hash {
        return hash
    }

    companion object : WithLogging()
}
