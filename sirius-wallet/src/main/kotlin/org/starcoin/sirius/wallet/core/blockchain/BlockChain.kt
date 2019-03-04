package org.starcoin.sirius.wallet.core.blockchain

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.*
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.wallet.core.ClientEventType
import org.starcoin.sirius.wallet.core.Hub
import org.starcoin.sirius.wallet.core.ResourceManager
import java.math.BigInteger

class BlockChain <T : ChainTransaction, A : ChainAccount> (chain: Chain<T, out Block<T>, A>, hub: Hub<T,A>, hubContract: HubContract<A>, account: A){

    private val chain = chain
    private val hub = hub
    private val contract = hubContract
    private val account = account
    internal var startWatch = false
    private var height : BigInteger =  BigInteger.ZERO

    private val syncedHeigth="synced-block-heigth".toByteArray()
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
                        ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                    LOG.info("$contractFunction: $input")
                    val withdrawalStatus = WithdrawalStatus( WithdrawalStatusType.INIT,input)
                    hub.onWithdrawal(withdrawalStatus)
                }
            }
            is CancelWithdrawalFunction -> {
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input")
                if(input.address.equals(account.address)){
                    hub.cancelWithdrawal(input)
                }
            }
            is OpenTransferDeliveryChallengeFunction -> {
                if(tx.from?.equals(account.address)?:false){
                    val input = contractFunction.decode(tx.data)
                        ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                    LOG.info("$contractFunction: $input")
                    hub.onTransferDeliveryChallenge(input)
                }
            }
            is OpenBalanceUpdateChallengeFunction -> {
                if(tx.from?.equals(account.address)?:false) {
                    val input = contractFunction.decode(tx.data)
                        ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                    LOG.info("$contractFunction: $input")
                    hub.onBalanceUpdateChallenge(input)
                }
            }
            is CommitFunction ->{
                val input = contractFunction.decode(tx.data)
                    ?: throw RuntimeException("$contractFunction decode tx:${txResult.tx} fail.")
                LOG.info("$contractFunction: $input ,status: ${txResult.receipt.status}")
                if(txResult.receipt.status)
                    hub.onHubRootCommit(input)
                else
                    GlobalScope.launch { hub.eonChannel?.send(ClientEventType.HUB_COMMIT_FAIL) }
            }
        }
    }

    fun watachBlock(){
        val heightBytes=ResourceManager.instance(account.address.toBytes().toHEXString()).dataStore.get(syncedHeigth)
        if(heightBytes!=null){
            this.height= heightBytes.toBigInteger()
        }
        GlobalScope.launch {
            val channel = chain.watchBlock(this@BlockChain.height)
            while (startWatch) {
                var block = channel.receive()
                val transactions=block.transactions
                for(transaction in transactions){
                    handleTransaction(transaction)
                }
                ResourceManager.instance(account.address.toBytes().toHEXString()).dataStore.put(syncedHeigth,height.toByteArray())
            }
        }
    }
}