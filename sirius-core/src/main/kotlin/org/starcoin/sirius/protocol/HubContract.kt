package org.starcoin.sirius.protocol

import org.starcoin.sirius.core.*
import kotlin.reflect.KClass

sealed class ContractFunction(val name: String, val inputClass: KClass<out SiriusObject>) {
    companion object {
        val functions = listOf(
            CommitFunction,
            InitiateWithdrawalFunction,
            CancelWithdrawalFunction,
            OpenBalanceUpdateChallengeFunction,
            CloseBalanceUpdateChallengeFunction,
            OpenTransferDeliveryChallengeFunction,
            CloseTransferDeliveryChallengeFunction,
            RecoverFundsFunction
        )
    }
}

object CommitFunction : ContractFunction("commit", HubRoot::class)
object InitiateWithdrawalFunction : ContractFunction("initiateWithdrawal", Withdrawal::class)
object CancelWithdrawalFunction : ContractFunction("cancelWithdrawal", CancelWithdrawal::class)
object OpenBalanceUpdateChallengeFunction :
    ContractFunction("openBalanceUpdateChallenge", BalanceUpdateChallenge::class)

object CloseBalanceUpdateChallengeFunction :
    ContractFunction("closeBalanceUpdateChallenge", CloseBalanceUpdateChallenge::class)

object OpenTransferDeliveryChallengeFunction :
    ContractFunction("openTransferDeliveryChallenge", TransferDeliveryChallenge::class)

object CloseTransferDeliveryChallengeFunction :
    ContractFunction("closeTransferDeliveryChallenge", CloseTransferDeliveryChallenge::class)

object RecoverFundsFunction : ContractFunction("recoverFundsFunction", AMTreeProof::class)

abstract class HubContract<A : ChainAccount> {

    abstract val contractAddress: Address

    fun queryHubInfo(account: A): ContractHubInfo {
        return this.queryContractFunction(account, "hubInfo", ContractHubInfo::class)
    }

    fun queryLeastHubCommit(account: A): HubRoot? {
        //TODO check has value.
        return this.queryContractFunction(account, "queryLeastHubCommit", HubRoot::class)
    }

    fun queryHubCommit(account: A, eon: Int): HubRoot? {
        return this.queryContractFunction(account, "queryHubCommit", HubRoot::class, eon)
    }

    fun queryCurrentBalanceUpdateChallenge(account: A, address: Address): BalanceUpdateChallenge? {
        return this.queryContractFunction(
            account,
            "queryCurrentBalanceUpdateChallenge",
            BalanceUpdateChallenge::class,
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

    fun openBalanceUpdateChallenge(account: A, input: BalanceUpdateChallenge): Hash {
        return this.executeContractFunction(
            account,
            OpenBalanceUpdateChallengeFunction, input
        )
    }

    fun closeBalanceUpdateChallenge(account: A, input: CloseBalanceUpdateChallenge): Hash {
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

    abstract fun <S : SiriusObject> executeContractFunction(account: A, function: ContractFunction, arguments: S): Hash

    abstract fun <S : SiriusObject> queryContractFunction(
        account: A,
        functionName: String,
        clazz: KClass<S>,
        vararg args: Any?
    ): S

}
