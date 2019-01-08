package org.starcoin.sirius.protocol.ethereum

import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.protocol.core.methods.response.EthBlock
import java.util.stream.Collectors

class EthereumBlock(override val height: Long, val hash: Hash) : Block<EthereumTransaction>() {

    override lateinit var transactions: MutableList<EthereumTransaction>

    constructor(ethBlock: EthBlock.Block) : this(ethBlock.number.toLong(), ethBlock.hash.toHash()) {
        transactions =
            ethBlock.transactions.map { EthereumTransaction(it.get() as org.web3j.protocol.core.methods.response.Transaction) }
                .stream()
                .collect(
                    Collectors.toList()
                )
    }

    constructor(ethBlock: org.ethereum.core.Block) : this(ethBlock.number, ethBlock.hash.toHash()) {
        transactions = ethBlock.transactionsList.map { EthereumTransaction(it) }.stream().collect(Collectors.toList())
    }

    override fun blockHash(): Hash {
        return hash
    }

}
