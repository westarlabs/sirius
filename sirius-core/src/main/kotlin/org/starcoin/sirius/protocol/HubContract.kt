package org.starcoin.sirius.protocol

import org.starcoin.sirius.chain.ChainStrategy
import org.starcoin.sirius.core.*
import kotlin.reflect.KClass

open class FunctionSignature(val value: ByteArray) {

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionSignature) return false

        if (!value.contentEquals(other.value)) return false

        return true
    }

    final override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

sealed class ContractFunction<S : SiriusObject>(val name: String, val inputClass: KClass<S>) {
    val signature by lazy { ChainStrategy.signature(this) }

    companion object {
        val functions by lazy {
            mapOf(
                CommitFunction.signature to
                        CommitFunction,
                InitiateWithdrawalFunction.signature to
                        InitiateWithdrawalFunction,
                CancelWithdrawalFunction.signature to
                        CancelWithdrawalFunction,
                OpenBalanceUpdateChallengeFunction.signature to
                        OpenBalanceUpdateChallengeFunction,
                CloseBalanceUpdateChallengeFunction.signature to
                        CloseBalanceUpdateChallengeFunction,
                OpenTransferDeliveryChallengeFunction.signature to
                        OpenTransferDeliveryChallengeFunction,
                CloseTransferDeliveryChallengeFunction.signature to
                        CloseTransferDeliveryChallengeFunction,
                RecoverFundsFunction.signature to
                        RecoverFundsFunction
            )
        }
    }

    fun decode(data: ByteArray?): S? {
        return data?.let { ChainStrategy.decode(this, data) }
    }

    fun encode(input: S): ByteArray {
        return ChainStrategy.encode(this, input)
    }

    override fun toString(): String {
        return name
    }

}

object CommitFunction : ContractFunction<HubRoot>("commit", HubRoot::class)
object InitiateWithdrawalFunction : ContractFunction<Withdrawal>("initiateWithdrawal", Withdrawal::class)
object CancelWithdrawalFunction : ContractFunction<CancelWithdrawal>("cancelWithdrawal", CancelWithdrawal::class)
object OpenBalanceUpdateChallengeFunction :
    ContractFunction<BalanceUpdateProof>("openBalanceUpdateChallenge", BalanceUpdateProof::class)

object CloseBalanceUpdateChallengeFunction :
    ContractFunction<AMTreeProof>("closeBalanceUpdateChallenge", AMTreeProof::class)

object OpenTransferDeliveryChallengeFunction :
    ContractFunction<TransferDeliveryChallenge>("openTransferDeliveryChallenge", TransferDeliveryChallenge::class)

object CloseTransferDeliveryChallengeFunction :
    ContractFunction<CloseTransferDeliveryChallenge>(
        "closeTransferDeliveryChallenge",
        CloseTransferDeliveryChallenge::class
    )

object RecoverFundsFunction : ContractFunction<AMTreeProof>("recoverFundsFunction", AMTreeProof::class)

abstract class HubContract<A : ChainAccount> {

    abstract val contractAddress: Address

    fun queryHubInfo(account: A): ContractHubInfo {
        return this.queryContractFunction(account, "hubInfo", ContractHubInfo::class)
    }

    fun getLatestRoot(account: A): HubRoot? {
        //TODO check has value.
        return this.queryContractFunction(account, "getLatestRoot", HubRoot::class)
    }

    fun queryHubCommit(account: A, eon: Int): HubRoot? {
        return this.queryContractFunction(account, "queryHubCommit", HubRoot::class, eon)
    }

    fun queryCurrentBalanceUpdateChallenge(account: A, address: Address): BalanceUpdateProof? {
        return this.queryContractFunction(
            account,
            "queryCurrentBalanceUpdateChallenge",
            BalanceUpdateProof::class,
            address
        )
    }

    fun queryCurrentTransferDeliveryChallenge(account: A, address: Address): TransferDeliveryChallenge? {
        return this.queryContractFunction(
            account,
            "queryCurrentTransferDeliveryChallenge",
            TransferDeliveryChallenge::class,
            address
        )
    }

    fun queryWithdrawalStatus(account: A, address: Address): WithdrawalStatus? {
        return this.queryContractFunction(
            account,
            "queryWithdrawalStatus",
            WithdrawalStatus::class,
            address
        )
    }

    fun initiateWithdrawal(account: A, input: Withdrawal): Hash {
        return this.executeContractFunction(account, InitiateWithdrawalFunction, input)
    }

    fun cancelWithdrawal(account: A, input: CancelWithdrawal): Hash {
        return this.executeContractFunction(account, CancelWithdrawalFunction, input)
    }

    fun openBalanceUpdateChallenge(account: A, input: BalanceUpdateProof): Hash {
        return this.executeContractFunction(
            account,
            OpenBalanceUpdateChallengeFunction, input
        )
    }

    fun closeBalanceUpdateChallenge(account: A, input: AMTreeProof): Hash {
        return this.executeContractFunction(
            account,
            CloseBalanceUpdateChallengeFunction, input
        )
    }

    fun commit(account: A, input: HubRoot): Hash {
        return this.executeContractFunction(account, CommitFunction, input)
    }

    fun openTransferDeliveryChallenge(account: A, input: TransferDeliveryChallenge): Hash {
        return this.executeContractFunction(
            account,
            OpenTransferDeliveryChallengeFunction, input
        )
    }

    fun closeTransferDeliveryChallenge(account: A, input: CloseTransferDeliveryChallenge): Hash {
        return this.executeContractFunction(
            account,
            CloseTransferDeliveryChallengeFunction, input
        )
    }

    fun recoverFunds(account: A, input: AMTreeProof): Hash {
        return this.executeContractFunction(account, RecoverFundsFunction, input)
    }

    abstract fun <S : SiriusObject> executeContractFunction(
        account: A,
        function: ContractFunction<S>,
        arguments: S
    ): Hash

    abstract fun <S : SiriusObject> queryContractFunction(
        account: A,
        functionName: String,
        clazz: KClass<S>,
        vararg args: Any
    ): S

    abstract fun setHubIp(account: A, ip: String)
}
