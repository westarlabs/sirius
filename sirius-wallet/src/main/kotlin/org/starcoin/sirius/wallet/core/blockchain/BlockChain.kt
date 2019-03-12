package org.starcoin.sirius.wallet.core.blockchain

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.protocol.*
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.wallet.core.ClientAccount
import org.starcoin.sirius.wallet.core.ClientEventType
import org.starcoin.sirius.wallet.core.Hub
import org.starcoin.sirius.wallet.core.ResourceManager
import java.math.BigInteger

class BlockChain <T : ChainTransaction, A : ChainAccount> (chain: Chain<T, out Block<T>, A>, hub: Hub<T,A>, hubContract: HubContract<A>, account: ClientAccount<A>){

    private val chain = chain
    private val hub = hub
    private val contract = hubContract
    private val account = account
    internal var startWatch = false

    lateinit  private var job :Job

    private val syncedHeight = "synced-block-height".toByteArray()
    companion object : WithLogging()

    fun watchTransaction(){
        GlobalScope.launch {
            val channnel=chain.watchTransactions {
                it.tx.from?.equals(account.address) ?: false || it.tx.from?.equals(contract.contractAddress) ?: false
                        || it.tx.to?.equals(account.address) ?: false || it.tx.to?.equals(contract.contractAddress) ?: false
            }
            while (startWatch) {
                val txResult = channnel.receive()
                handleTransaction(txResult)
            }
        }
    }

    suspend fun handleTransaction(txResult:TransactionResult<T>){
        val tx = txResult.tx
        //val hash = tx.hash()
        //txReceipts[hash]?.complete(txResult.receipt)
        val contractFunction = tx.contractFunction
        when (contractFunction) {
            null -> {
                if(tx.from?.equals(account.address)?:false){
                    val deposit = Deposit(tx.from!!, tx.amount)
                    LOG.info("Deposit:" + deposit.toJSON())
                    hub.confirmDeposit(tx)
                    GlobalScope.launch { hub.eonChannel?.send(ClientEventType.DEPOSIT) }
                }
            }
            is InitiateWithdrawalFunction -> {
                if(tx.from?.equals(account.address)?:false){
                    val input = contractFunction.decode(tx.data)
                        ?: fail { "$contractFunction decode tx:${txResult.tx} fail." }
                    LOG.info("$contractFunction: $input,transaction result is ${txResult.receipt.status}")
                    val withdrawalStatus = WithdrawalStatus( WithdrawalStatusType.INIT,input)
                    hub.onWithdrawal(withdrawalStatus)
                }
            }
            is CancelWithdrawalFunction -> {
                val input = contractFunction.decode(tx.data)
                    ?: fail { "$contractFunction decode tx:${txResult.tx} fail." }
                LOG.info("$contractFunction: $input ,transaction result is ${txResult.receipt.status}")
                if(input.address.equals(account.address)){
                    hub.cancelWithdrawal(input)
                }
            }
            is OpenTransferDeliveryChallengeFunction -> {
                if(tx.from?.equals(account.address)?:false){
                    val input = contractFunction.decode(tx.data)
                        ?: fail { "$contractFunction decode tx:${txResult.tx} fail." }
                    LOG.info("$contractFunction: $input ,transaction result is ${txResult.receipt.status}")
                    hub.onTransferDeliveryChallenge(input)
                }
            }
            is OpenBalanceUpdateChallengeFunction -> {
                if(tx.from?.equals(account.address)?:false) {
                    val input = contractFunction.decode(tx.data)
                        ?: fail { "$contractFunction decode tx:${txResult.tx} fail." }
                    LOG.info("$contractFunction: $input,transaction result is ${txResult.receipt.status}")
                    hub.onBalanceUpdateChallenge(input)
                }
            }
            is CommitFunction ->{
                val input = contractFunction.decode(tx.data)
                    ?: fail { "$contractFunction decode tx:${txResult.tx} fail." }
                LOG.info("$contractFunction: $input ,status: ${txResult.receipt.status}")
                if(txResult.receipt.status)
                    hub.onHubRootCommit(input)
                else
                    GlobalScope.launch { hub.eonChannel?.send(ClientEventType.HUB_COMMIT_FAIL) }
            }
        }
    }

    fun watachBlock(height:BigInteger){
        job=GlobalScope.launch {
            val channel = chain.watchBlock(height)
            while (startWatch) {
                var block = channel.receive()
                val transactions=block.transactions
                for(transaction in transactions){
                    handleTransaction(transaction)
                }
                ResourceManager.instance(account.name).dataStore.put(
                    syncedHeight,
                    block.height.toBigInteger().toByteArray()
                )
            }
        }
    }

    fun getLocalHeight():BigInteger{
        val heightBytes = ResourceManager.instance(account.name).dataStore.get(syncedHeight)
        return heightBytes?.toBigInteger()?:BigInteger.ZERO
    }

    internal suspend fun close(){
        job.cancel()
    }

    fun chainBalance():BigInteger{
        return chain.getBalance(account.address)
    }
}