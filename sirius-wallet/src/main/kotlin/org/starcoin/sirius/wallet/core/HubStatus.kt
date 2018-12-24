package org.starcoin.sirius.wallet.core

import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import java.security.KeyPair

class HubStatus {

    var allotment: Long = 0

    var eonStatuses = arrayListOf<EonStatus>()

    @Volatile
    var currentEonStatusIndex = 0

    val lastIndex = -1

    var depositingTransactions: MutableMap<Hash, ChainTransaction> = mutableMapOf()

    var withdrawalStatus: WithdrawalStatus? = null
        set(value) {
            if (withdrawalStatus == null) {
                this.withdrawalStatus = null
                return
            }
            if (this.withdrawalStatus?.withdrawalAmount==value?.withdrawalAmount
                && this.withdrawalStatus?.status === org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE
            ) {
                return
            }
            if (value?.status != org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE || value.status != org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL_VALUE)
                this.withdrawalStatus = withdrawalStatus
        }

    var height: Int = 0

    fun HubStatus(eon: Eon) {
        val eonStatus = EonStatus(eon, 0)
        this.eonStatuses[currentEonStatusIndex] = eonStatus
    }

    internal fun syncWithDrawal(withdrawalStatus: WithdrawalStatus) {
        this.withdrawalStatus = withdrawalStatus
    }

    internal fun cancelWithDrawal() {
        this.allotment += this.withdrawalStatus?.withdrawalAmount?:0
        this.withdrawalStatus = null
    }

    internal fun confirmDeposit(chainTransaction: ChainTransaction) {
        this.eonStatuses[currentEonStatusIndex].confirmedTransactions.add(chainTransaction)
        this.depositingTransactions.remove(chainTransaction.hash())
    }

    internal fun addUpdate(update: UpdateData) {
        this.eonStatuses[currentEonStatusIndex].updateHistory.add(update)
    }

    internal fun currentUpdate(eon: Eon): UpdateData {
        val updateList = this.eonStatuses[currentEonStatusIndex].updateHistory
        if (updateList.size == 0) {
            return UpdateData(eon.id, 0, 0, 0, null)
        } else {
            val index = this.eonStatuses[currentEonStatusIndex].updateHistory.size - 1
            return updateList[index]
        }
    }

    internal fun addOffchainTransaction(transaction: OffchainTransaction) {
        this.eonStatuses[currentEonStatusIndex].transactionHistory.add(transaction)
        this.eonStatuses[currentEonStatusIndex].transactionMap.put(
            transaction.hash().toMD5Hex(), transaction
        )
    }

    internal fun transactionTree(): MerkleTree<OffchainTransaction>{
        return MerkleTree(eonStatuses[currentEonStatusIndex].transactionHistory)
    }

    internal fun transactionPath(hash: Hash): MerklePath<OffchainTransaction>? {
        val merkleTree = MerkleTree(eonStatuses[getEonByIndex(lastIndex)].transactionHistory)
        return merkleTree.getMembershipProof(hash)
    }

    internal fun getTransactionByHash(hash: String): OffchainTransaction? {
        return eonStatuses[getEonByIndex(lastIndex)].transactionMap[hash]
    }

    internal fun currentEonPath(): AugmentedMerklePath? {
        val eonStatus = eonStatuses[currentEonStatusIndex]
        return eonStatus?.path
    }

    internal fun currentTransactions(): List<OffchainTransaction> {
        return eonStatuses[currentEonStatusIndex].transactionHistory
    }

    internal fun getCurrentEonStatus(eon: Eon): EonStatus? {
        for (eonStatus in this.eonStatuses) {
            if (eonStatus != null && eonStatus.eon.id === eon.id) {
                return eonStatus
            }
        }
        return null
    }

    internal fun findCurrentEonStatusIndex(eon: Eon): Int {
        for (i in 0..2) {
            val eonStatus = this.eonStatuses[i]
            if (eonStatus != null && eonStatus.eon.id.equals(eon.id)) {
                return i
            }
        }
        return -1
    }

    internal fun depositTransaction(chainTransaction: ChainTransaction) {
        this.depositingTransactions[chainTransaction.hash()] = chainTransaction
    }

    internal fun nextEon(eon: Eon, path: AugmentedMerklePath): Int {
        this.allotment += this.eonStatuses[currentEonStatusIndex]
            .confirmedTransactions
            .stream()
            .mapToLong { t -> t.amount }
            .sum()
        this.allotment += this.currentUpdate(eon).receiveAmount
        this.allotment -= this.currentUpdate(eon).sendAmount
        if ((this.withdrawalStatus?.status == org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_PASSED_VALUE || this.withdrawalStatus?.status === org.starcoin.proto.Starcoin.ProtoWithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE)
            && this.withdrawalStatus?.eon === eon.id - 2
        ) {
            this.allotment -= this.withdrawalStatus?.withdrawalAmount?:0
            this.withdrawalStatus?.clientConfirm()
        }

        val lastIndex = currentEonStatusIndex
        val maybe = currentEonStatusIndex + 1
        if (maybe > 2) {
            currentEonStatusIndex = 0
        } else {
            currentEonStatusIndex = maybe
        }
        this.eonStatuses[currentEonStatusIndex] = EonStatus(eon, this.allotment)

        this.eonStatuses[currentEonStatusIndex].path = path
        return lastIndex
    }

    internal fun newChallenge(update: UpdateData, keyPair: KeyPair, lastIndex: Int): BalanceUpdateChallenge? {
        var challenge: BalanceUpdateChallenge? = null
        if (eonStatuses[lastIndex] != null && eonStatuses[lastIndex].path != null) {
            challenge = BalanceUpdateChallenge(null, eonStatuses[lastIndex].path, keyPair.public)
        } else {
            challenge = BalanceUpdateChallenge(update, null, keyPair.public)
        }
        return challenge
    }

    internal fun syncAllotment(accountInfo: Starcoin.ProtoHubAccount) {
        this.allotment += accountInfo.deposit
        this.allotment += accountInfo.allotment
    }

    @Synchronized
    internal fun getEonByIndex(i: Int): Int {
        if (i > 0 || i < -2) {
            return -3
        } else {
            val index = currentEonStatusIndex + i
            return if (index < 0) {
                index + 2
            } else {
                index
            }
        }
    }

    fun findMaxEon(): Eon {
        var maxEonStatus = this.eonStatuses[0]
        for (eonStatus in this.eonStatuses) {
            if (eonStatus == null) {
                continue
            }
            if (maxEonStatus.eon.id < eonStatus.eon.id) {
                maxEonStatus = eonStatus
            }
        }
        return maxEonStatus.eon
    }


}
