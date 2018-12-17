package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.*
import org.ethereum.listener.EthereumListener
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction

class InMemoryEthereumListener : EthereumListener {

    val blocks : MutableList<BlockInfo> = mutableListOf();

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
        blockSummary?.block
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

    fun Transaction.chainTransaction(): ChainTransaction {
        return ChainTransaction(
            // Transaction from block address
            BlockAddress.valueOf(this.sender),
            // Transaction to block address
            BlockAddress.valueOf(this.receiveAddress),
            // Transaction timestamp
            0,  //timestamp
            // Transaction value
            this.value as Long, // value
            // Transaction data
            "",
            // FIXME: No argument in ethereum transaction
            // Transaction argument
            this.data
        )
    }

    fun BlockSummary.blockInfo(): BlockInfo {
        val blockInfo = BlockInfo(this.block?.number as Int)
        this.block?.transactionsList?.map { it ->
            blockInfo.addTransaction(
                ChainTransaction(
                    // Transaction from block address
                    BlockAddress.valueOf(it.sender),
                    // Transaction to block address
                    BlockAddress.valueOf(it.receiveAddress),
                    // Transaction timestamp
                    0,  //timestamp
                    // Transaction value
                    it.value as Long, // value
                    // Transaction data
                    "",
                    // FIXME: No argument in ethereum transaction
                    // Transaction argument
                    it.data
                )
            )
        }
        return blockInfo
    }

}