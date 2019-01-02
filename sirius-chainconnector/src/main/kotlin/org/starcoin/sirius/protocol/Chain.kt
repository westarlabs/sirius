package org.starcoin.sirius.protocol

import kotlinx.coroutines.channels.Channel
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import java.math.BigInteger

enum class ChainType {
    ETH
}

class FilterArguments


data class TransactionResult<T : ChainTransaction>(val tx: T, val receipt: Receipt) {

}

enum class EventTopic {
    Deposit,
    WithDraw
}

interface Chain<T : ChainTransaction, B : Block<T>, C : HubContract> {

    fun findTransaction(hash: Hash): T?

    fun getBlock(height: BigInteger = BigInteger.valueOf(-1)): B?

    fun watchBlock(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean={true}
    ): Channel<B>

    fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt>

    fun watchTransactions(
        contract: Address,
        topic: EventTopic,
        filter: (FilterArguments) -> Boolean={true}):Channel<TransactionResult<T>>

    fun getBalance(address: Address): BigInteger

    fun newTransaction(key: CryptoKey, transaction: T)

    fun getContract(parameter: QueryContractParameter): C

}
