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

interface Chain <T:ChainTransaction,B:BlockInfo,C:HubContract>{

    fun findTransaction(hash: Hash): T?

    fun getBlock(height: BigInteger): B?

    fun watchBlock(onNext: ((block: B) -> Unit))

    fun watchTransactions(onNext: ((tx: T) -> Unit))

    fun getBalance(address: BlockAddress): BigInteger

    fun newTransaction(keyPair: KeyPair,transaction: T)

    fun watchTransaction(txHash: Hash, listener: TransactionProgressListener)

    fun getContract(): C
}