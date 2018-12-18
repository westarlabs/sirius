package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.Hash
import java.math.BigInteger
import java.security.KeyPair

//watch transaction process
interface TransactionProgressListener {

}

interface Chain {

    fun findTransaction(hash: Hash): ChainTransaction?

    fun getBlock(height: BigInteger): BlockInfo?

    fun watchBlock(onNext: ((block: BlockInfo) -> Unit))

    fun watchTransactions(onNext: ((tx: ChainTransaction) -> Unit))

    fun getBalance(address: BlockAddress): BigInteger

    fun newTransaction(keyPair: KeyPair,transaction: ChainTransaction)

    fun watchTransaction(txHash: Hash, listener: TransactionProgressListener)

    fun getContract(): HubContract
}