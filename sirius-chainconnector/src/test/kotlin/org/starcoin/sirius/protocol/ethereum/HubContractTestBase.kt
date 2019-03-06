package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ChainEvent
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.protocol.ethereum.contract.EthereumHubContract
import org.starcoin.sirius.util.MockUtils
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger
import kotlin.properties.Delegates


abstract class HubContractTestBase {
    companion object : WithLogging()

    protected abstract val chain: EthereumBaseChain
    private var contract: EthereumHubContract by Delegates.notNull()
    private var owner: EthereumAccount by Delegates.notNull()
    private var alice: EthereumAccount by Delegates.notNull()
    private var ownerChannel: ReceiveChannel<TransactionResult<EthereumTransaction>> by Delegates.notNull()
    private var aliceChannel: ReceiveChannel<TransactionResult<EthereumTransaction>> by Delegates.notNull()
    private var blocksPerEon = ContractConstructArgs.DEFAULT_ARG.blocksPerEon
    private var startBlockNumber: Long by Delegates.notNull()

    @Before
    fun beforeTest() {

        owner = EthereumAccount(CryptoService.generateCryptoKey())
        alice = EthereumAccount(CryptoService.generateCryptoKey())

        val amount = EtherUtil.convert(10, EtherUtil.Unit.ETHER)
        sendEther(owner, amount)
        sendEther(alice, amount)

        this.contract = chain.deployContract(owner, ContractConstructArgs.DEFAULT_ARG)

        aliceChannel =
                chain.watchTransactions { it.tx.from == alice.address && it.tx.to == contract.contractAddress }
        ownerChannel =
                chain.watchTransactions { it.tx.from == owner.address && it.tx.to == contract.contractAddress }
        val hubInfo = this.contract.queryHubInfo(owner)
        startBlockNumber = hubInfo.startBlockNumber.longValueExact()
    }


    @Test
    open fun testCurrentEon() {
        Assert.assertEquals(contract.getCurrentEon(owner), 0)
    }

    @Test
    fun testIsRecoveryMode() {
        Assert.assertEquals(contract.isRecoveryMode(owner), false)
    }

    @Test
    @ImplicitReflectionSerializer
    open fun testDeposit() {

        val amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)

        deposit(alice, amount)

        runBlocking {
            val transaction = aliceChannel.receive()
            Assert.assertTrue(transaction.receipt.status)
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, contract.contractAddress)
            Assert.assertEquals(transaction.tx.amount, amount)
        }

        Assert.assertEquals(
            amount,
            chain.getBalance(contract.contractAddress)
        )
    }

    @Test
    @ImplicitReflectionSerializer
    open fun testWithDrawal() = runBlocking {
        var amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI)
        deposit(alice, amount)
        runBlocking {
            val transaction = aliceChannel.receive()
            Assert.assertTrue(transaction.receipt.status)
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, contract.contractAddress)
            Assert.assertEquals(transaction.tx.amount, amount)
        }
        val eon = 1
        val path = newProof(alice.address, newUpdate(eon, 1, BigInteger.ZERO, alice), BigInteger.ZERO, amount)

        val contractAddr = contract.contractAddress
        amount = EtherUtil.convert(8, EtherUtil.Unit.GWEI)
        val withdrawal = Withdrawal(path, amount)
        var deferred = contract.initiateWithdrawal(alice, withdrawal)
        deferred.await()
        var transaction = chain.findTransaction(deferred.txHash)

        Assert.assertEquals(transaction?.from, alice.address)
        Assert.assertEquals(transaction?.to, contractAddr)
        amount = EtherUtil.convert(2, EtherUtil.Unit.ETHER)

        val update = newUpdate(eon, 2, amount, alice.key)
        val proof = newProof(alice.address, newUpdate(eon, 1, BigInteger.ZERO, alice), BigInteger.ZERO, amount)

        val cancel =
            CancelWithdrawal(alice.address, update, proof)
        deferred = contract.cancelWithdrawal(owner, cancel)
        deferred.await()
        transaction = chain.findTransaction(deferred.txHash)
        Assert.assertEquals(transaction?.from, owner.address)
        Assert.assertEquals(transaction?.to, contractAddr)

    }

    private fun newPath(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreePath {
        val path = AMTreePath(update.eon, newLeaf(addr, update, offset, allotment))
        for (i in 0..MockUtils.nextInt(0, 10)) {
            path.append(AMTreePathNode.mock())
        }

        return path
    }

    private fun newProof(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreeProof {
        return AMTreeProof(newPath(addr, update, offset, allotment), AMTreeLeafNodeInfo(addr.hash(), update))
    }

    private fun newLeaf(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreePathNode {
        val nodeInfo = newLeafNodeInfo(addr, update)
        return AMTreePathNode(nodeInfo.hash(), PathDirection.LEFT, offset, allotment)
    }

    private fun newLeafNodeInfo(addr: Address, update: Update): AMTreeLeafNodeInfo {
        return AMTreeLeafNodeInfo(addr.hash(), update)
    }

    private fun newUpdate(eon: Int, version: Long, sendAmount: BigInteger, callUser: EthereumAccount): Update {
        return newUpdate(eon, version, sendAmount, callUser.key)
    }

    private fun newUpdate(eon: Int, version: Long, sendAmount: BigInteger, callUser: CryptoKey): Update {
        val updateData = UpdateData(eon, version, sendAmount, 0.toBigInteger(), Hash.random())
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)
        return update
    }

    @Test
    @ImplicitReflectionSerializer
    open fun testCommit() {
        val eventChannel = chain.watchEvents(contract.contractAddress, listOf(ChainEvent.ReturnEvent))
        val amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI)
        deposit(alice, amount)
        runBlocking {
            val txResult = aliceChannel.receive()
            Assert.assertTrue(txResult.receipt.status)
            Assert.assertEquals(txResult.tx.from, alice.address)
            Assert.assertEquals(txResult.tx.to, contract.contractAddress)
            Assert.assertEquals(txResult.tx.amount, amount)
        }
        Assert.assertEquals(
            amount,
            chain.getBalance(contract.contractAddress)
        )
        val eon = 1
        val root = newHubRoot(eon, amount)
        commitHubRoot(1, root)
        val root1 = contract.getLatestRoot(EthereumAccount.DUMMY_ACCOUNT)
        Assert.assertEquals(root, root1)
        GlobalScope.launch {
            eventChannel.consumeEach { LOG.info("watched:${it.receipt.logs}") }
        }
    }

    private fun newHubRoot(eon: Int, amount: BigInteger): HubRoot {
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathNode(info.hash(), PathDirection.ROOT, 0.toBigInteger(), amount)
        return HubRoot(node, eon)
    }

    private fun waitToEon(eon: Int) {
        if (eon == 0) {
            return
        }
        val currentBlockNumber = chain.getBlockNumber()
        chain.waitBlocks(Eon.waitToEon(startBlockNumber, currentBlockNumber, blocksPerEon, eon))
    }

    private fun commitHubRoot(eon: Int, amount: BigInteger): Hash {
        return commitHubRoot(eon, newHubRoot(eon, amount))
    }

    private fun commitHubRoot(eon: Int, root: HubRoot): Hash {
        LOG.info("current block height is :${chain.getBlockNumber()}, commit root: $root")
        waitToEon(eon)
        val deferred = contract.commit(owner, root)

        runBlocking {
            deferred.await()
            val txResult = ownerChannel.receive()
            Assert.assertTrue(txResult.receipt.status)
            Assert.assertEquals(deferred.txHash, txResult.tx.hash())
            val transaction = txResult.tx
            Assert.assertEquals(contract.contractAddress, transaction.to)
            Assert.assertEquals(owner.address, transaction.from)
        }
        return deferred.txHash
    }

    @Test
    @ImplicitReflectionSerializer
    open fun testHubInfo() {
        val ip = "192.168.0.0.1:80"
        contract.setHubIp(owner, ip)

        val hubInfo = contract.queryHubInfo(EthereumAccount.DUMMY_ACCOUNT)
        Assert.assertNotNull(hubInfo)
        Assert.assertEquals(hubInfo.hubAddress, ip)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testBalanceUpdateChallenge() {
        runBlocking {
            val amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)
            deposit(alice, amount)
            commitHubRoot(1, amount)
            val update1 = newUpdate(0, 1, BigInteger.ZERO, alice.key)//other
            val path = newPath(alice.address, update1, BigInteger.ZERO, amount)
            val update2 = newUpdate(0, 1, BigInteger.ZERO, alice.key)//mine
            val leaf2 = newLeafNodeInfo(alice.address, update2)
            val amtp = AMTreeProof(path, leaf2)
            val bup = BalanceUpdateProof(true, update2, true, amtp.path)
            var deferred = contract.openBalanceUpdateChallenge(alice, bup)
            deferred.await()
            val tx = chain.findTransaction(deferred.txHash)
            Assert.assertNotNull(tx)
            val update4 = newUpdate(0, 4, BigInteger.ZERO, alice)//mine
            val leaf3 = newLeafNodeInfo(alice.address, update4)
            val amtp2 = AMTreeProof(path, leaf3)
            deferred = contract.closeBalanceUpdateChallenge(owner, CloseBalanceUpdateChallenge(alice.address, amtp2))
            deferred.await()
        }
    }

    @Test
    @ImplicitReflectionSerializer
    fun testTransferChallenge() {
        runBlocking {
            val amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)
            deposit(alice, amount)
            commitHubRoot(1, amount)
            val update = newUpdate(0, 1, BigInteger.ZERO, alice)
            val txData = OffchainTransactionData(0, alice.address, owner.address, 10, 1)
            val tx = OffchainTransaction(txData)
            tx.sign(alice.key)
            val open = TransferDeliveryChallenge(update, tx, MerklePath.mock())
            var deferred = contract.openTransferDeliveryChallenge(alice, open)
            deferred.await()
            val update1 = newUpdate(0, 1, BigInteger.ZERO, alice)//other
            val path = newPath(alice.address, update1, BigInteger.ZERO, 200.toBigInteger())
            val update2 = newUpdate(0, 1, BigInteger.ZERO, alice)//mine
            val leaf2 = newLeafNodeInfo(alice.address, update2)
            val amtp = AMTreeProof(path, leaf2)
            val close =
                CloseTransferDeliveryChallenge(amtp, MerklePath.mock(), alice.address, Hash.of(tx))
            deferred = contract.closeTransferDeliveryChallenge(owner, close)
            deferred.await()
        }
    }

    private fun deposit(alice: EthereumAccount, amount: BigInteger) = runBlocking {
        val ethereumTransaction = EthereumTransaction(
            contract.contractAddress, alice.getNonce(), 21000.toBigInteger(),
            210000.toBigInteger(), amount
        )
        val deferred = chain.submitTransaction(alice, ethereumTransaction)
        deferred.await()
    }

    abstract fun sendEther(to: EthereumAccount, value: BigInteger)
}