package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bouncycastle.util.BigIntegers
import org.ethereum.core.*
import org.ethereum.listener.EthereumListener
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.utils.Numeric
import java.math.BigInteger
import kotlin.properties.Delegates

class InMemoryEthereumListener : EthereumListener {

    val ZEROBYTES=ByteArray(0)

    val blocks: MutableList<EthereumBlock> = mutableListOf()
    var blockChannel :kotlinx.coroutines.channels.Channel<EthereumBlock> by Delegates.notNull()

    override fun onSyncDone(state: EthereumListener.SyncState?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSendMessage(channel: Channel?, message: Message?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPendingStateChanged(pendingState: PendingState?) {
        println(pendingState)
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBlock(blockSummary: BlockSummary?) {
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
                it.key.pubKey.toHEXString(),it.encodedRaw.toString(),it.signature.r.toString(),it.signature.s.toString(),it.signature.v.toInt())
        }
        GlobalScope.launch {
            blockChannel.send(EthereumBlock(w3jBlock))
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        println(output)
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
        return blocks.flatMap { it.getTransactions() }.first { it.equals(hash) }
    }
}
