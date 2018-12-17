package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import java.math.BigInteger


interface Chain {

    fun findTransaction(hash: ByteArray): ChainTransaction?

    fun getBlock(height: BigInteger): BlockInfo?

    fun watchBlock(onNext: ((block: BlockInfo) -> Unit))

    fun watchTransaction(onNext: ((tx: ChainTransaction) -> Unit))

    fun getBalance(address: ByteArray): BigInteger
}