package org.starcoin.sirius.protocol.ethereum

import org.bouncycastle.util.BigIntegers
import org.ethereum.core.*
import org.ethereum.listener.EthereumListener
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.protocol.EthereumTransaction
import org.web3j.protocol.core.methods.response.EthBlock
import kotlin.properties.Delegates

class InMemoryEthereumListener : EthereumListener {

    val blocks: MutableList<EthereumBlock> = mutableListOf()
    var blockChannel :kotlinx.coroutines.channels.Channel<EthereumBlock> by Delegates.notNull()

    override fun onSyncDone(state: EthereumListener.SyncState?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSendMessage(channel: Channel?, message: Message?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPendingStateChanged(pendingState: PendingState?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        val block = blockSummary?.blockInfo()
        if (block != null) {
            blocks.add(block)
        }
        var w3jBlock = EthBlock.Block()
        w3jBlock.setGasLimit(String(blockSummary?.block?.gasLimit?: ByteArray(0)))
        w3jBlock.setDifficulty(blockSummary?.block?.difficultyBI?.toString())
        w3jBlock.setGasUsed(String.format("%d",blockSummary?.block?.gasUsed?:0))
        w3jBlock.setNonce(BigIntegers.fromUnsignedByteArray(blockSummary?.block?.nonce?:ByteArray(0)).toString())
        w3jBlock.setNumber(String.format("%d",blockSummary?.block?.number?:0))
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
