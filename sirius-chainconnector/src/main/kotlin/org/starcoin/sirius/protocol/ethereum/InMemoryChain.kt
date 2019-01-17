package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.channels.Channel
import org.ethereum.core.CallTransaction.createRawTransaction
import org.ethereum.solidity.SolidityType
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.EventTopic
import org.starcoin.sirius.protocol.TransactionResult
import java.math.BigInteger

class InMemoryChain(autoGenblock: Boolean = true) : EthereumBaseChain() {

    override fun watchEvents(
        contract: Address,
        topic: EventTopic,
        filter: (TransactionResult<EthereumTransaction>) -> Boolean
    ): Channel<TransactionResult<EthereumTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun watchTransactions(filter: (TransactionResult<EthereumTransaction>) -> Boolean): Channel<TransactionResult<EthereumTransaction>> {
        var transactionChannel = Channel<TransactionResult<EthereumTransaction>>(200)
        inMemoryEthereumListener.transactionFilter = filter
        inMemoryEthereumListener.transactionChannel = transactionChannel
        sb.addEthereumListener(inMemoryEthereumListener)
        if (autoGenblock) {
            sb.withAutoblock(autoGenblock)
        }
        return transactionChannel
    }

    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val autoGenblock = autoGenblock
    val sb = StandaloneBlockchain().withAutoblock(autoGenblock).withGasLimit(500000000)

    private val inMemoryEthereumListener = InMemoryEthereumListener()

    override fun getBlock(height: BigInteger): EthereumBlock? {
        return inMemoryEthereumListener.blocks.get(height.toInt())
    }

    override fun watchBlock(filter: (EthereumBlock) -> Boolean): Channel<EthereumBlock> {
        //TODO support filter.
        var blockChannel = Channel<EthereumBlock>(200)
        inMemoryEthereumListener.blockChannel = blockChannel
        sb.addEthereumListener(inMemoryEthereumListener)
        if (autoGenblock) {
            sb.withAutoblock(autoGenblock)
        }
        return blockChannel
    }

    override fun getBalance(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getBalance(address.toBytes())
    }

    override fun getNonce(address: Address): BigInteger {
        return sb.getBlockchain().getRepository().getNonce(address.toBytes())
    }

    override fun findTransaction(hash: Hash): EthereumTransaction? {
        return inMemoryEthereumListener.findTransaction(hash)
    }

    override fun submitTransaction(account: EthereumAccount, transaction: EthereumTransaction): Hash {
        val ecKey = (account.key as EthCryptoKey).ecKey
        sb.sender = ecKey
        transaction.tx.sign(ecKey)
        sb.submitTransaction(transaction.tx)
        account.getAndIncNonce()
        return transaction.tx.rawHash.toHash()
    }

    val bytesType: SolidityType.BytesType = SolidityType.BytesType()

    override fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray {
        val tx = createRawTransaction(0, 0, 100000000000000L, contractAddress.toBytes().toHEXString(), 0, data)
        tx.sign((caller as EthCryptoKey).ecKey)
        val callBlock = sb.blockchain.bestBlock
        val repository = this.sb.blockchain.getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking()

        try {
            val executor = org.ethereum.core.TransactionExecutor(
                tx, callBlock.getCoinbase(), repository, sb.blockchain.getBlockStore(),
                sb.blockchain.getProgramInvokeFactory(), callBlock
            )
                .setLocalCall(true)

            executor.init()
            executor.execute()
            executor.go()
            executor.finalization()
            if (executor.result.isRevert || executor.result.exception != null) {
                //TODO define custom error.
                throw RuntimeException("callConstFunction fail")
            }
            val bytes = executor.result.hReturn
            return bytesType.decode(bytes, SolidityType.IntType.decodeInt(bytes, 0).toInt()) as ByteArray
        } finally {
            repository.rollback()
        }
    }

    override fun getBlockNumber(): BigInteger {
        return inMemoryEthereumListener.currentNumber.toBigInteger()
    }

    override fun newTransaction(account: EthereumAccount,to:Address,value:BigInteger):EthereumTransaction {
        var ethereumTransaction = EthereumTransaction(
            to, account.getNonce(), 21000.toBigInteger(),
            210000.toBigInteger(), value
        )
        return ethereumTransaction
    }

    fun createBlock(): EthereumBlock {
        val block = this.sb.createBlock()
        return EthereumBlock(block)
    }

    fun miningCoin(address: Address, amount: BigInteger) {
        this.sb.sendEther(address.toBytes(), amount)
        this.sb.createBlock()
    }
}
