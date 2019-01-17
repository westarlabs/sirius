package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.core.HubRoot.Companion.EMPTY_TREE_HUBROOT
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.TransactionResult
import org.starcoin.sirius.protocol.ethereum.contract.EthereumHubContract
import org.starcoin.sirius.util.MockUtils
import java.math.BigInteger
import kotlin.properties.Delegates

class InMemoryHubContractTest {

    private var chain: InMemoryChain by Delegates.notNull()
    private var contract: EthereumHubContract by Delegates.notNull()

    private var owner: EthereumAccount by Delegates.notNull()
    private var alice: EthereumAccount by Delegates.notNull()

    private var transactionChannel: Channel<TransactionResult<EthereumTransaction>> by Delegates.notNull()

    @Before
    fun beforeTest() {
        chain = InMemoryChain(true)
        transactionChannel =
            chain.watchTransactions { it.tx.from == alice.address && it.tx.to == contract.contractAddress }
        owner = EthereumAccount(CryptoService.generateCryptoKey())
        alice = EthereumAccount(CryptoService.generateCryptoKey())

        val amount = EtherUtil.convert(100000, EtherUtil.Unit.ETHER)
        this.sendEther(owner.address, amount)
        this.sendEther(alice.address, amount)
        val args = ContractConstructArgs(8, EMPTY_TREE_HUBROOT)
        this.contract = chain.deployContract(owner, args)
        commitHubRoot(0, BigInteger.ZERO)
    }

    fun sendEther(address: Address, amount: BigInteger) {
        chain.sb.sendEther(address.toBytes(), amount)
        chain.sb.createBlock()
        Assert.assertEquals(amount, chain.getBalance(address))
    }

    @Ignore
    @Test
    @ImplicitReflectionSerializer
    fun testCurrentEon() {
        Assert.assertEquals(contract.getCurrentEon(), 0)
    }

    fun deposit(alice: EthereumAccount, amount: BigInteger) {
        var ethereumTransaction = EthereumTransaction(
            contract.contractAddress, alice.getNonce(), 21000.toBigInteger(),
            210000.toBigInteger(), amount
        )

        chain.submitTransaction(alice, ethereumTransaction)

        //chain.sb.sendEther(alice.address.toBytes(), BigInteger.valueOf(1))
        chain.sb.createBlock()
    }

    @Test
    @ImplicitReflectionSerializer
    fun testDeposit() {

        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)

        deposit(alice, amount)

        runBlocking {
            var transaction = transactionChannel.receive()
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
    fun testWithDrawal() {

        //chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        //println(chain.sb.getBlockchain().getRepository().getBalance(alice.address.toBytes()))

        var amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI)
        deposit(alice, amount)

        runBlocking {
            var transaction = transactionChannel.receive()
            Assert.assertTrue(transaction.receipt.status)
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, contract.contractAddress)
            Assert.assertEquals(transaction.tx.amount, amount)
        }

        /**
        var hash=commitHubRoot(0,amount)

        println(chain.getNumber())
        println(contract.getCurrentEon())
        var transaction=chain.findTransaction(hash)
        Assert.assertEquals(transaction?.to,contract.contractAddress)
        Assert.assertEquals(transaction?.from,Address.wrap(chain.sb.sender.address))*/

        val eon = 1
        val path = newProof(alice.address, newUpdate(eon, 1, BigInteger.ZERO, alice), BigInteger.ZERO, amount)

        var contractAddr = contract.contractAddress

        //var owner = chain.sb.sender
        //chain.sb.sender = (alice as EthCryptoKey).ecKey

        amount = EtherUtil.convert(8, EtherUtil.Unit.GWEI)
        val withdrawal = Withdrawal(alice.address, path, amount)
        var hash = contract.initiateWithdrawal(alice, withdrawal)
        //TODO use feature to wait.
        Thread.sleep(500)
        var transaction = chain.findTransaction(hash)

        Assert.assertEquals(transaction?.from, alice.address)
        Assert.assertEquals(transaction?.to, contractAddr)

        //chain.sb.sender = owner

        amount = EtherUtil.convert(2, EtherUtil.Unit.ETHER)

        val update = newUpdate(eon, 2, amount, alice.key)
        val proof = newProof(alice.address, newUpdate(eon, 1, BigInteger.ZERO, alice), BigInteger.ZERO, amount)

        val cancel =
            CancelWithdrawal(alice.address, update, proof)
        hash = contract.cancelWithdrawal(owner, cancel)
        transaction = chain.findTransaction(hash)
        //TODO use feature to wait.
        Thread.sleep(500)
        Assert.assertEquals(transaction?.from, owner.address)
        Assert.assertEquals(transaction?.to, contractAddr)

    }

    private fun newPath(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreePath {
        val path = AMTreePath(update.eon, newLeaf(addr, update, offset, allotment))
        for (i in 0..MockUtils.nextInt(0, 10)) {
            path.append(AMTreePathInternalNode.mock())
        }

        return path
    }

    private fun newProof(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreeProof {
        return  AMTreeProof(newPath(addr,update,offset,allotment),newLeaf(addr,update,offset,allotment))
    }

    private fun newLeaf(addr: Address, update: Update, offset: BigInteger, allotment: BigInteger): AMTreePathLeafNode {
        val nodeInfo = AMTreeLeafNodeInfo(addr.hash(), update)
        return AMTreePathLeafNode(nodeInfo, PathDirection.LEFT, offset, allotment)
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
    fun testCommit() {

        var amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI)

        //println(chain.sb.getBlockchain().getRepository().getBalance(alice.address.toBytes()))
        deposit(alice, amount)

        runBlocking {
            var txResult = transactionChannel.receive()
            Assert.assertTrue(txResult.receipt.status)
            Assert.assertEquals(txResult.tx.from, alice.address)
            Assert.assertEquals(txResult.tx.to, contract.contractAddress)
            Assert.assertEquals(txResult.tx.amount, amount)
        }

        Assert.assertEquals(
            amount,
            chain.getBalance(contract.contractAddress)
        )

        //println(chain.sb.getBlockchain().getRepository().getBalance(contract.contractAddress))
        var hash = commitHubRoot(1, amount)
        println(hash)
        //TODO use feature to wait.
        Thread.sleep(500)
        var transaction = chain.findTransaction(hash)
        Assert.assertEquals(transaction?.to, contract.contractAddress)
        Assert.assertEquals(transaction?.from, Address.wrap(chain.sb.sender.address))

        var root = contract.getLatestRoot(EthereumAccount.DUMMY_ACCOUNT)
        println(root)
    }

    private fun commitHubRoot(eon: Int, amount: BigInteger): Hash {
        var height = chain.getBlockNumber().toLong()
        if (eon != 0) {
            if (height?.rem(8) != 0L) {
                var blockNumber = 8 - (height?.rem(8) ?: 0) - 1
                for (i in 0..blockNumber) {
                    chain.sb.createBlock()
                }
            }
        }
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0.toBigInteger(), amount)
        val root = HubRoot(node, eon)
        println("current block height is :" + chain.getBlockNumber())
        println(root)
        val callResult = contract.commit(owner, root)
        return callResult
    }

    @Test
    @ImplicitReflectionSerializer
    fun testHubInfo() {
        var ip = "192.168.0.0.1:80"
        contract.setHubIp(owner, ip)

        var hubInfo = contract.queryHubInfo(EthereumAccount.DUMMY_ACCOUNT)
        Assert.assertNotNull(hubInfo)
        Assert.assertEquals(hubInfo.hubAddress, ip)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testBalanceUpdateChallenge() {

        //var transactions = List<EthereumTransaction>
        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)

        deposit(alice, amount)

        commitHubRoot(1, amount)

        //var owner = chain.sb.sender
        //chain.sb.sender = (alice as EthCryptoKey).ecKey

        val update1 = newUpdate(0, 1, BigInteger.ZERO, alice.key)//other
        val path = newPath(alice.address, update1, BigInteger.ZERO, amount)
        val update2 = newUpdate(0, 1, BigInteger.ZERO, alice.key)//mine
        val leaf2 = newLeaf(alice.address, update2, 1100.toBigInteger(), 1000.toBigInteger())
        val amtp = AMTreeProof(path, leaf2)
        val bup = BalanceUpdateProof(true, update2, true, amtp)
        val buc = BalanceUpdateChallenge(bup, alice.key.keyPair.public)

        var hash = contract.openBalanceUpdateChallenge(owner, buc)

        //TODO
        Thread.sleep(500)
        var transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

        //chain.sb.sender = owner
        val update3 = newUpdate(0, 3, BigInteger.ZERO, alice)//other
        val update4 = newUpdate(0, 4, BigInteger.ZERO, alice)//mine
        val path3 = newPath(alice.address, update3, BigInteger.ZERO, 20.toBigInteger())
        val leaf3 = newLeaf(alice.address, update4, 1100.toBigInteger(), 1000.toBigInteger())
        val amtp2 = AMTreeProof(path, leaf3)
        val close = CloseBalanceUpdateChallenge(update4, amtp2)
        hash = contract.closeBalanceUpdateChallenge(alice, close)
        //TODO
        Thread.sleep(500)
        transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

    }

    @Test
    @ImplicitReflectionSerializer
    fun testTransferChallenge() {

        //var transactions = List<EthereumTransaction>
        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI)

        deposit(alice, amount)

        commitHubRoot(1, amount)

        //var owner = chain.sb.sender
        //chain.sb.sender = (alice as EthCryptoKey).ecKey

        val update = newUpdate(0, 1, BigInteger.ZERO, alice)
        val txData = OffchainTransactionData(0, alice.address, owner.address, 10, 1)
        val tx = OffchainTransaction(txData)
        tx.sign(alice.key)
        val open = TransferDeliveryChallenge(update, tx, MerklePath.mock())
        var hash = contract.openTransferDeliveryChallenge(alice, open)
        //TODO
        Thread.sleep(500)
        var transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

        //chain.sb.sender = owner

        val update1 = newUpdate(0, 1, BigInteger.ZERO, alice)//other
        val path = newPath(alice.address, update1, BigInteger.ZERO, 200.toBigInteger())
        val update2 = newUpdate(0, 1, BigInteger.ZERO, alice)//mine
        val leaf2 = newLeaf(alice.address, update2, 1100.toBigInteger(), 1000.toBigInteger())
        val amtp = AMTreeProof(path, leaf2)
        val close =
            CloseTransferDeliveryChallenge(amtp, update2, MerklePath.mock(), alice.key.keyPair.public, Hash.of(tx))
        hash = contract.closeTransferDeliveryChallenge(owner, close)
        //TODO
        Thread.sleep(500)
        transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)
    }
}
