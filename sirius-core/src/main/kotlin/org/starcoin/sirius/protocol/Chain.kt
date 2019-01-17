package org.starcoin.sirius.protocol

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.serialization.ProtobufSchema
import java.io.File
import java.math.BigInteger

data class TransactionResult<T : ChainTransaction>(val tx: T, val receipt: Receipt)

@Serializable
@ProtobufSchema(Starcoin.ContractConstructArgs::class)
data class ContractConstructArgs(@SerialId(1) val blocks: Long, @SerialId(2) val hubRoot: HubRoot) : SiriusObject() {
    companion object :
        SiriusObjectCompanion<ContractConstructArgs, Starcoin.ContractConstructArgs>(ContractConstructArgs::class) {

        override fun mock(): ContractConstructArgs {
            return ContractConstructArgs(8, HubRoot.mock())
        }
    }
}

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

    fun deployContract(account: A, args: ContractConstructArgs): HubContract<A>

    fun deployContract(account: A, contractFile: File, args: ContractConstructArgs): HubContract<A>

    fun getBlockNumber(): BigInteger

    fun newTransaction(account: A, to: Address, value: BigInteger): T
}
