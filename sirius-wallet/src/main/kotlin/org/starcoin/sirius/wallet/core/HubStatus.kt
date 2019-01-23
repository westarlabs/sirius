package org.starcoin.sirius.wallet.core

import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toBigInteger
import java.math.BigInteger

class HubStatus {

    var allotment: BigInteger = BigInteger.ZERO
        private set

    private var eonStatuses = Array(3){EonStatus()}

    var blocksPerEon: Int = 0
        internal set

    @Volatile
    private var currentEonStatusIndex = 0

    private val lastIndex = -1

    private var depositingTransactions: MutableMap<Hash, ChainTransaction> = mutableMapOf()

    private var withdrawalStatus: WithdrawalStatus? = null

    var height: Int = 0

    internal constructor(eon: Eon) {
        val eonStatus = EonStatus(eon, BigInteger.ZERO)

        this.eonStatuses[currentEonStatusIndex] = eonStatus
    }

    internal fun syncWithDrawal(value: WithdrawalStatus) {
        if (value == null) {
            this.withdrawalStatus = null
            return
        }
        if (this.withdrawalStatus?.withdrawalAmount==value?.withdrawalAmount
            && this.withdrawalStatus?.status === Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE
        ) {
            return
        }
        if (value?.status != Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE || value.status != Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CANCEL_VALUE)
            this.withdrawalStatus = value
    }

    internal fun cancelWithDrawal() {
        this.allotment += this.withdrawalStatus?.withdrawalAmount ?: BigInteger.ZERO
        this.withdrawalStatus = null
    }

    internal fun confirmDeposit(transaction: ChainTransaction) {
        this.allotment+=transaction.amount
        this.eonStatuses[currentEonStatusIndex].addDeposit(transaction)
        this.depositingTransactions.remove(transaction.hash())
    }

    internal fun addDepositTransaction(hash:Hash,transaction: ChainTransaction){
        this.depositingTransactions.put(hash,transaction)
    }

    internal fun addUpdate(update: Update) {
        this.eonStatuses[currentEonStatusIndex].updateHistory.add(update)
    }

    internal fun currentUpdate(eon: Eon): Update {
        val updateList = this.eonStatuses[currentEonStatusIndex].updateHistory
        if (updateList.size == 0) {
            return Update(eon.id, 0, 0, 0)
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

    internal fun transactionTree(): MerkleTree{
        return MerkleTree(eonStatuses[currentEonStatusIndex].transactionHistory)
    }

    internal fun transactionPath(hash: Hash): MerklePath? {
        val merkleTree = MerkleTree(eonStatuses[getEonByIndex(lastIndex)].transactionHistory)
        return merkleTree.getMembershipProof(hash)
    }

    internal fun getTransactionByHash(hash: String): OffchainTransaction? {
        return eonStatuses[getEonByIndex(lastIndex)].transactionMap[hash]
    }

    internal fun currentEonProof(): AMTreeProof? {
        val eonStatus = eonStatuses[currentEonStatusIndex]
        return eonStatus?.treeProof
    }

    internal fun lastEonProof(): AMTreeProof? {
        val eonStatus = eonStatuses[getEonByIndex(lastIndex)]
        return eonStatus?.treeProof
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

    internal fun nextEon(eon: Eon, path: AMTreeProof): Int {
//        this.allotment += this.eonStatuses[currentEonStatusIndex]
//            .confirmedTransactions
//            .stream()
//            .map { it.amount }.reduce(BigInteger.ZERO) { a, b -> a.add(b) }
        this.allotment += this.currentUpdate(eon).receiveAmount
        this.allotment -= this.currentUpdate(eon).sendAmount
        if ((this.withdrawalStatus?.status == Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_PASSED_VALUE || this.withdrawalStatus?.status === Starcoin.WithdrawalStatusType.WITHDRAWAL_STATUS_CLIENT_CONFIRMED_VALUE)
            && this.withdrawalStatus?.eon === eon.id - 2
        ) {
            this.allotment -= this.withdrawalStatus?.withdrawalAmount ?: BigInteger.ZERO
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

        this.eonStatuses[currentEonStatusIndex].treeProof = path
        return lastIndex
    }

    internal fun newChallenge(update: Update,lastIndex: Int):BalanceUpdateProof {
        if (eonStatuses[lastIndex] != null && eonStatuses[lastIndex].treeProof != null) {
            return BalanceUpdateProof(eonStatuses[lastIndex].treeProof!!.path!!)
        } else {
            return BalanceUpdateProof(update)
        }
    }

    internal fun syncAllotment(accountInfo: Starcoin.HubAccount) {
        this.allotment += accountInfo.deposit.toByteArray().toBigInteger()
        this.allotment += accountInfo.allotment.toByteArray().toBigInteger()
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

    internal fun findMaxEon(): Eon {
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

    fun couldWithDrawal():Boolean {
        if (this.withdrawalStatus == null) return true else return false
    }

    internal fun addWithDrawal(value:WithdrawalStatus){
    }
}
