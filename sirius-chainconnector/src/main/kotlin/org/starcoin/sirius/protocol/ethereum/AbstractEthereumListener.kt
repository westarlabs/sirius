package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.*
import org.ethereum.listener.EthereumListener
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.util.WithLogging

abstract class AbstractEthereumListener : EthereumListener {

    val traceMap = mutableMapOf<Hash, String>()

    override fun onSyncDone(state: EthereumListener.SyncState) {
    }

    override fun onSendMessage(channel: Channel, message: Message) {
    }

    override fun onPendingStateChanged(pendingState: PendingState) {
    }

    override fun onRecvMessage(channel: Channel, message: Message) {
    }

    override fun onPendingTransactionUpdate(
        txReceipt: TransactionReceipt,
        state: EthereumListener.PendingTransactionState,
        block: Block
    ) {
    }

    override fun onVMTraceCreated(transactionHash: String, trace: String) {
        //LOG.fine("chain vm trace $transactionHash $trace")
        traceMap[transactionHash.hexToByteArray().toHash()] = trace
    }

    override fun onBlock(blockSummary: BlockSummary) {
    }

    override fun onPeerDisconnect(host: String, port: Long) {
    }

    override fun onPeerAddedToSyncPool(peer: Channel) {
    }

    override fun onPendingTransactionsReceived(transactions: MutableList<Transaction>) {
    }

    override fun onTransactionExecuted(summary: TransactionExecutionSummary) {
    }

    override fun onNodeDiscovered(node: Node) {
    }

    override fun onHandShakePeer(channel: Channel, helloMessage: HelloMessage) {
    }

    override fun onEthStatusUpdated(channel: Channel, status: StatusMessage) {
    }

    override fun trace(output: String) {
        LOG.fine("chain trace $output")
    }

    override fun onNoConnections() {
    }

    companion object : WithLogging()
}
