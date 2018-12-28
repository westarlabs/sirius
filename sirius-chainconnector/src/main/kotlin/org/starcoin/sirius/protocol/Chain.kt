package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import java.math.BigInteger

enum class ChainType {
    ETH
}

class FilterArguments {
    
}

data class TransactionResult<T : ChainTransaction>(val tx: T, val receipt: Receipt) {

}

interface Chain<T : ChainTransaction, B : Block<T>, C : HubContract> {

    fun findTransaction(hash: Hash): T?

    fun getBlock(height: BigInteger = BigInteger.valueOf(-1)): B?

    fun watchBlock(filter: (FilterArguments) -> Boolean, onNext: (block: B) -> Unit)

    fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt>

    fun watchTransactions(filter: (FilterArguments) -> Boolean, onNext: (txResult: TransactionResult<T>) -> Unit)

    fun getBalance(address: Address): BigInteger

    fun newTransaction(key: CryptoKey, transaction: T)

    fun getContract(parameter: QueryContractParameter): C

}
