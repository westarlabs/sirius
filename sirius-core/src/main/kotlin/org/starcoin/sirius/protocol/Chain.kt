package org.starcoin.sirius.protocol

import kotlinx.coroutines.channels.ReceiveChannel
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import java.io.File
import java.math.BigInteger

data class TransactionResult<T : ChainTransaction>(val tx: T, val receipt: Receipt)

enum class ChainEvent private constructor(val eventSignature: String) {
    MockTopic("DepositEvent(byte[])"),
    SiriusEvent("SiriusEvent(bytes32,uint,bytes)"),
    ReturnEvent("ReturnEvent(bool)");
}

interface Chain<T : ChainTransaction, B : Block<T>, A : ChainAccount> {

    fun findTransaction(hash: Hash): T?

    fun getBlock(height: BigInteger = BigInteger.valueOf(-1)): B?

    fun watchBlock(filter: (B) -> Boolean = { true }): ReceiveChannel<B>

    fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt?>

    fun watchTransactions(filter: (TransactionResult<T>) -> Boolean = { true }): ReceiveChannel<TransactionResult<T>>

    fun watchEvents(
        contract: Address,
        events: Collection<ChainEvent>,
        filter: (TransactionResult<T>) -> Boolean = { true }
    ): ReceiveChannel<TransactionResult<T>>

    fun getBalance(address: Address): BigInteger

    fun submitTransaction(account: A, transaction: T): TxDeferred

    fun loadContract(contractAddress: Address, jsonInterface: String): HubContract<A>

    fun loadContract(contractAddress: Address): HubContract<A>

    fun deployContract(account: A, args: ContractConstructArgs): HubContract<A>

    fun getBlockNumber(): Long

    fun newTransaction(account: A, to: Address, value: BigInteger): T

    fun waitBlocks(blockCount: Int = 1)

    fun stop()
    fun start()
    fun getNonce(address: Address): BigInteger

    fun waitTransactionProcessed(hash: Hash): TxDeferred

    fun createAccount(key: CryptoKey): A

    fun createAccount(keystore: File, password: String): A

    fun createAccount(): A

    /**
     * try to pre mining coin to account, if chain implements not support, just return false.
     */
    fun tryMiningCoin(account: A, amount: BigInteger): Boolean
}
