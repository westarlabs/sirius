package org.starcoin.sirius.protocol

import kotlinx.coroutines.channels.Channel
import org.starcoin.sirius.core.*
import java.io.File
import java.math.BigInteger

data class TransactionResult<T : ChainTransaction>(val tx: T, val receipt: Receipt)

enum class EventTopic(val event: String) {
    Deposit("DepositEvent(byte[])")
}

interface Chain<T : ChainTransaction, B : Block<T>, A : ChainAccount> {

    fun findTransaction(hash: Hash): T?

    fun getBlock(height: BigInteger = BigInteger.valueOf(-1)): B?

    fun watchBlock(filter: (B) -> Boolean = { true }): Channel<B>

    fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt>

    fun watchTransactions(filter: (TransactionResult<T>) -> Boolean = { true }): Channel<TransactionResult<T>>

    fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (TransactionResult<T>) -> Boolean = { true }
    ): Channel<TransactionResult<T>>

    fun getBalance(address: Address): BigInteger

    fun submitTransaction(account: A, transaction: T): Hash

    fun loadContract(contractAddress: Address, jsonInterface: String): HubContract<A>

    fun loadContract(contractAddress: Address): HubContract<A>
    
    fun deployContract(account: A): HubContract<A>

    fun deployContract(account: A, contractFile: File): HubContract<A>

    fun getBlockNumber(): BigInteger

    fun newTransaction(account: A,to:Address,value:BigInteger):T
}
