package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bouncycastle.util.BigIntegers
import org.ethereum.core.*
import org.ethereum.listener.EthereumListener
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.web3j.utils.Numeric
import java.math.BigInteger
import kotlin.properties.Delegates
import org.ethereum.core.CallTransaction
import org.ethereum.util.blockchain.SolidityContract


class InMemoryEthereumListener : EthereumListener {

    internal val blocks: MutableList<EthereumBlock> = mutableListOf()

    internal val transactions : MutableMap<Hash,EthereumTransaction> = mutableMapOf()
    internal var blockChannel :kotlinx.coroutines.channels.Channel<EthereumBlock> by Delegates.notNull()
    internal var transactionChannel : kotlinx.coroutines.channels.Channel<TransactionResult<EthereumTransaction>> by Delegates.notNull()

    internal var transactionFilter : (TransactionResult<EthereumTransaction>) -> Boolean by Delegates.notNull()

    internal var currentNumber :Long?=0

    override fun onSyncDone(state: EthereumListener.SyncState?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSendMessage(channel: Channel?, message: Message?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPendingStateChanged(pendingState: PendingState?) {
        //println(pendingState)
    }

    override fun onRecvMessage(channel: Channel?, message: Message?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPendingTransactionUpdate(
        txReceipt: TransactionReceipt?,
        state: EthereumListener.PendingTransactionState?,
        block: Block?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onVMTraceCreated(transactionHash: String?, trace: String?) {
        //println(transactionHash)
        //println(trace)
    }

    override fun onBlock(blockSummary: BlockSummary?) {
        /**
        var w3jBlock = EthBlock.Block()
        w3jBlock.setGasLimit(blockSummary?.block?.gasLimit?.toBigIntString())
        w3jBlock.setDifficulty(blockSummary?.block?.difficultyBI?.toString())
        w3jBlock.setGasUsed(blockSummary?.block?.gasUsed.toString())
        w3jBlock.setNonce(blockSummary?.block?.nonce?.toBigIntString())
        w3jBlock.setNumber(blockSummary?.block?.number?.toBigIntString())
        w3jBlock.hash = blockSummary?.block?.hash?.toHEXString()
        w3jBlock.transactions=blockSummary?.block?.transactionsList?.mapIndexed{index, it->
            EthBlock.TransactionObject(it.hash?.toHEXString(),it.nonce?.toString(),w3jBlock.hash,w3jBlock.number.toString(),
                index.toString(),it.sender.toString(),it.receiveAddress.toString(),it.value.toBigIntString(),
                it.gasPrice.toBigIntString(),it.gasLimit.toBigIntString(),it.data?.toString(),blockSummary?.block?.timestamp.toString(),
                it.key?.pubKey?.toHEXString(),it.encodedRaw.toString(),it.signature.r.toString(),it.signature.s.toString(),it.signature.v.toInt())
        }*/
        currentNumber=blockSummary?.block?.number
        GlobalScope.launch {
            //blockChannel.send(EthereumBlock(w3jBlock))
            blockSummary?.block?.transactionsList?.forEachIndexed{ index,it->
                var ethereumTransaction=EthereumTransaction(it)
                val transactionResult=TransactionResult(ethereumTransaction, Receipt(it.hash,BigInteger.valueOf(index.toLong()),
                    blockSummary.block.hash, BigInteger.valueOf(blockSummary.block.number),null,it.sender,it.receiveAddress,
                    BigInteger.valueOf(blockSummary.block.header.gasUsed), blockSummary.block.header.logsBloom.toHEXString(),
                    BigInteger.valueOf(0),blockSummary.block.header.receiptsRoot.toHEXString(),true))
                if(transactionFilter(transactionResult))
                    transactionChannel.send(transactionResult)
                transactions.put(Hash.of(it.hash),ethereumTransaction)
            }
        }
    }

    fun ByteArray.toBigIntString():String{
        return BigIntegers.fromUnsignedByteArray(this).toString()
    }

    fun Long.toBigIntString():String{
        return Numeric.encodeQuantity(BigInteger.valueOf(this))
    }

    override fun onPeerDisconnect(host: String?, port: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPeerAddedToSyncPool(peer: Channel?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPendingTransactionsReceived(transactions: MutableList<Transaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTransactionExecuted(summary: TransactionExecutionSummary?) {
//        val abi =
//            "[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"to\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"amount\",\"type\":\"uint128\"}],\"name\":\"Transfer\",\"type\":\"event\"}]\n"
        //contract?.
        summary?.logs?.forEach {
            //val contract = CallTransaction.Contract(abi)
            //val invocation = contract.parseEvent(it)
            //println(invocation)
            println(it)
        }
    }

    override fun onNodeDiscovered(node: Node?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHandShakePeer(channel: Channel?, helloMessage: HelloMessage?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onEthStatusUpdated(channel: Channel?, status: StatusMessage?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun trace(output: String?) {
        //println(output)
    }

    override fun onNoConnections() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun Transaction.chainTransaction(): EthereumTransaction {
        return EthereumTransaction(this)
    }

    fun BlockSummary.blockInfo(): EthereumBlock {
        TODO()
    }

    fun findTransaction(hash: Hash): EthereumTransaction? {
        return transactions.get(hash)
    }
}
