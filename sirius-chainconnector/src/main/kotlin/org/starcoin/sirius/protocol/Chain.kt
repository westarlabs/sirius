package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.Hash
import java.math.BigInteger
import java.security.KeyPair


interface Chain {

    fun findTransaction(hash: Hash): ChainTransaction?

    fun getBlock(height: BigInteger): BlockInfo?

    fun watchBlock(onNext: ((block: BlockInfo) -> Unit))

    fun watchTransaction(onNext: ((tx: ChainTransaction) -> Unit))

    fun getBalance(address: BlockAddress): BigInteger

    fun newTransaction(keyPair: KeyPair,transaction: ChainTransaction)

}