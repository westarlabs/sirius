package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.Transaction
import org.starcoin.sirius.core.Block
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.protocol.core.methods.response.EthBlock
import java.util.stream.Collectors

class EthereumBlock(val ethBlock: EthBlock.Block) : Block<EthereumTransaction>(
    ethBlock.number.toLong(),
    //TODO
    ethBlock.transactions.stream().map { EthereumTransaction(it.get() as Transaction) }.collect(
        Collectors.toList()
    )
) {

    override fun blockHash(): Hash {
        return Hash.wrap(ethBlock.hash)
    }

}
