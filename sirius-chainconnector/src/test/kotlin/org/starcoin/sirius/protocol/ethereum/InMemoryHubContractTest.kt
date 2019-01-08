package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.contract.InMemoryHubContract
import org.starcoin.sirius.util.MockUtils
import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

class InMemoryHubContractTest {

    private var chain: InMemoryChain by Delegates.notNull()
    private var contract: InMemoryHubContract by Delegates.notNull()

    fun loadResource(name: String): URL {
        var resource = this.javaClass::class.java.getResource(name)
        if (resource == null) {
            var path = File("./out/test/resources" + name)
            //println(path.absolutePath)
            resource = path.toURL()
        }
        //println(resource)
        return resource
    }

    @Before
    fun beforeTest() {
        chain = InMemoryChain(true)
        //chain.sb.withGasLimit(500000000000000)
        val compiler = SolidityCompiler(SystemProperties.getDefault())

        val solRResource = loadResource("/solidity/sirius.sol")

        val solUri = solRResource.toURI()

        val path = File(solUri).parentFile.absolutePath
        //println("allowed_path:$path")

        val contractName = "SiriusService"
        val compileRes = compiler.compileSrc(
            File(solUri),
            true,
            true,
            SolidityCompiler.Options.ABI,
            SolidityCompiler.Options.BIN,
            SolidityCompiler.Options.AllowPaths(listOf(path))
        )
        if (compileRes.isFailed()) throw RuntimeException("Compile result: " + compileRes.errors)

        val result = CompilationResult.parse(compileRes.output)

        //var con= result.getContract(contractName)
        contract = InMemoryHubContract(chain.sb.submitNewContract(result.getContract(contractName)), chain.sb.sender)
        commitHubRoot(0, 0)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testCurrentEon() {
        Assert.assertEquals(contract.getCurrentEon(), 0)
    }

    fun deposit(alice: CryptoKey, nonce: AtomicInteger, amount: Long) {
        var ethereumTransaction = EthereumTransaction(
            Address.wrap(contract.getContractAddr()), nonce.getAndIncrement().toLong(), 21000,
            210000, amount, null
        )

        chain.newTransaction(alice, ethereumTransaction)

        //chain.sb.sendEther(alice.address.toBytes(), BigInteger.valueOf(1))
        chain.sb.createBlock()
    }

    @Test
    @ImplicitReflectionSerializer
    fun testDeposit() {

        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        var transactionChannel =
            chain.watchTransactions({ it.tx.from == alice.address && it.tx.to == Address.wrap(contract.getContractAddr()) })
        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI).toLong()

        chain.sb.sendEther(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        chain.sb.createBlock()

        deposit(alice, nonce, amount)

        runBlocking {
            var transaction = transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(), amount)
        }

        Assert.assertEquals(chain.sb.getBlockchain().getRepository().getBalance(contract.getContractAddr()).toLong(),amount)

    }

    @Test
    @ImplicitReflectionSerializer
    fun testWithDrawal() {
        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        var transactionChannel =
            chain.watchTransactions({ it.tx.from == alice.address && it.tx.to == Address.wrap(contract.getContractAddr()) })

        chain.sb.sendEther(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        chain.sb.createBlock()

        //chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        //println(chain.sb.getBlockchain().getRepository().getBalance(alice.address.toBytes()))

        var amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI).toLong()
        deposit(alice, nonce, amount)

        runBlocking {
            var transaction = transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(), amount)
        }

        val eon = 1

        commitHubRoot(1,amount)
        val path = newPath(alice.address, newUpdate(eon, 1, 0, alice),0,amount)
        var contractAddr = contract.getContractAddr()

        var owner = chain.sb.sender
        chain.sb.sender = (alice as EthCryptoKey).ecKey

        amount = EtherUtil.convert(8, EtherUtil.Unit.GWEI).toLong()
        val withdrawal = Withdrawal(alice.address, path, amount)
        var hash = contract.initiateWithdrawal(withdrawal)

        var transaction = chain.findTransaction(hash)

        Assert.assertEquals(transaction?.from, alice.address)
        Assert.assertEquals(transaction?.to, Address.wrap(contractAddr))

        chain.sb.sender = owner

        amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI).toLong()

        val update = newUpdate(eon, 2, amount, alice)
        val cancel =
            CancelWithdrawal(alice.address, update, path)
        hash = contract.cancelWithdrawal(cancel)
        transaction = chain.findTransaction(hash)

        Assert.assertEquals(transaction?.from, Address.wrap(owner.address))
        Assert.assertEquals(transaction?.to, Address.wrap(contractAddr))

    }

    private fun newPath(addr: Address, update: Update,offset: Long,allotment: Long): AMTreePath {
        val path = AMTreePath(update.eon, newLeaf(addr, update, offset, allotment))
        for (i in 0..MockUtils.nextInt(0, 10)) {
            path.append(AMTreePathInternalNode.mock())
        }

        return path
    }

    private fun newLeaf(addr: Address, update: Update, offset: Long, allotment: Long): AMTreePathLeafNode {
        val nodeInfo = AMTreeLeafNodeInfo(addr.hash(), update)
        return AMTreePathLeafNode(nodeInfo, PathDirection.LEFT, offset, allotment)
    }

    private fun newUpdate(eon: Int, version: Long, sendAmount: Long, callUser: CryptoKey): Update {
        val updateData = UpdateData(eon, version, sendAmount, 0, Hash.random())
        val update = Update(updateData)
        update.sign(callUser)
        update.signHub(callUser)
        return update
    }

    @Test
    @ImplicitReflectionSerializer
    fun testCommit() {
        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        chain.sb.sendEther(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        chain.sb.createBlock()

        var transactionChannel =
            chain.watchTransactions({ it.tx.from == alice.address && it.tx.to == Address.wrap(contract.getContractAddr()) })
        var amount = EtherUtil.convert(1000, EtherUtil.Unit.GWEI).toLong()

        //println(chain.sb.getBlockchain().getRepository().getBalance(alice.address.toBytes()))
        deposit(alice, nonce, amount)

        runBlocking {
            var transaction = transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from, alice.address)
            Assert.assertEquals(transaction.tx.to, Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(), amount)
        }

        Assert.assertEquals(chain.sb.getBlockchain().getRepository().getBalance(contract.getContractAddr()).toLong(),amount)

        //println(chain.sb.getBlockchain().getRepository().getBalance(contract.getContractAddr()))
        var hash = commitHubRoot(1, amount)

        var transaction = chain.findTransaction(hash)
        Assert.assertEquals(transaction?.to, Address.wrap(contract.getContractAddr()))
        Assert.assertEquals(transaction?.from, Address.wrap(chain.sb.sender.address))

        var root = contract.queryLeastHubCommit()

        println(root)
        Assert.assertEquals(root.eon, 1)
        Assert.assertEquals(root.root.allotment.toLong(), amount)

    }

    private fun commitHubRoot(eon: Int, amount: Long): Hash {
        var height = chain.getNumber()
        if (eon != 0) {
            if (height?.rem(8) != 0L) {
                var blockNumber = 8 - (height?.rem(8) ?: 0) - 1
                for (i in 0..blockNumber) {
                    chain.sb.createBlock()
                }
            }
        }
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0, amount)
        val root = HubRoot(node, eon)
        println("current block height is :"+chain.getNumber())
        println(root)
        val callResult = contract.commit(root)
        return callResult
    }

    @Test
    @ImplicitReflectionSerializer
    fun testHubInfo() {
        var ip = "192.168.0.0.1:80"
        contract.hubIp(ip)

        var hubInfo = contract.queryHubInfo()
        Assert.assertEquals(hubInfo.hubAddress, ip)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testBalanceUpdateChallenge() {
        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        var transactionChannel =
            chain.watchTransactions({ it.tx.from == alice.address && it.tx.to == Address.wrap(contract.getContractAddr()) })

        //var transactions = List<EthereumTransaction>
        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI).toLong()

        chain.sb.sendEther(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        chain.sb.createBlock()

        deposit(alice, nonce, amount)

        commitHubRoot(1,amount)

        var owner = chain.sb.sender
        chain.sb.sender = (alice as EthCryptoKey).ecKey

        val update1 = newUpdate(0, 1, 0,alice)//other
        val path = newPath(alice.address, update1,0,amount)
        val update2 = newUpdate(0, 1, 0,alice)//mine
        val leaf2 = newLeaf(alice.address, update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val bup = BalanceUpdateProof(true, update2, true, amtp)
        val buc = BalanceUpdateChallenge(bup, alice.keyPair.public)
        var hash=contract.openBalanceUpdateChallenge(buc)

        var transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

        chain.sb.sender = owner
        val update3 = newUpdate(0, 3, 0,alice)//other
        val update4 = newUpdate(0, 4, 0,alice)//mine
        val path3 = newPath(alice.address, update3,0,20)
        val leaf3 = newLeaf(alice.address, update4, 1100, 1000)
        val amtp2 = AMTreeProof(path, leaf3)
        val close = CloseBalanceUpdateChallenge(update4, amtp2)
        hash = contract.closeBalanceUpdateChallenge(close)

        transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

    }

    @Test
    @ImplicitReflectionSerializer
    fun testTransferChallenge() {
        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        var transactionChannel =
            chain.watchTransactions({ it.tx.from == alice.address && it.tx.to == Address.wrap(contract.getContractAddr()) })

        //var transactions = List<EthereumTransaction>
        var amount = EtherUtil.convert(100, EtherUtil.Unit.GWEI).toLong()

        chain.sb.sendEther(alice.address.toBytes(), EtherUtil.convert(100000, EtherUtil.Unit.ETHER))
        chain.sb.createBlock()

        deposit(alice, nonce, amount)

        commitHubRoot(1,amount)

        var owner = chain.sb.sender
        chain.sb.sender = (alice as EthCryptoKey).ecKey

        val update = newUpdate(0, 1, 0,alice)
        val txData = OffchainTransactionData(0, alice.address, Address.wrap(owner.address), 10, 1)
        val tx = OffchainTransaction(txData)
        tx.sign(alice)
        val open = TransferDeliveryChallenge(update, tx, MerklePath.mock())
        var hash = contract.openTransferDeliveryChallenge(open)

        var transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)

        chain.sb.sender = owner

        val update1 = newUpdate(0, 1, 0,alice)//other
        val path = newPath(alice.address, update1,0,200)
        val update2 = newUpdate(0, 1, 0,alice)//mine
        val leaf2 = newLeaf(alice.address, update2, 1100, 1000)
        val amtp = AMTreeProof(path, leaf2)
        val close =
            CloseTransferDeliveryChallenge(amtp, update2, MerklePath.mock(), alice.keyPair.public, Hash.of(tx))

        hash = contract.closeTransferDeliveryChallenge(close)

        transaction = chain.findTransaction(hash)
        Assert.assertNotNull(transaction)
    }
}