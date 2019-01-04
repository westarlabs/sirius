package org.starcoin.sirius.protocol.ethereum

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.EventTopic
import org.starcoin.sirius.protocol.ethereum.contract.InMemoryHubContract
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.MockUtils
import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

class InMemoryHubContractTest {

    private var chain : InMemoryChain by Delegates.notNull()
    private var contract : InMemoryHubContract by Delegates.notNull()

    fun loadResource(name:String): URL {
        var resource = this.javaClass::class.java.getResource(name)
        if(resource==null){
            var path=File("./out/test/resources"+name)
            //println(path.absolutePath)
            resource=path.toURL()
        }
        //println(resource)
        return resource
    }

    @Before
    fun beforeTest(){
        chain = InMemoryChain(true)
        //chain.sb.withGasLimit(500000000000000)
        val compiler = SolidityCompiler(SystemProperties.getDefault())

        val solRResource= loadResource("/solidity/sirius.sol")

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
        contract = InMemoryHubContract(chain.sb.submitNewContract(result.getContract(contractName)),chain.sb.sender)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testCurrentEon(){
        Assert.assertEquals(contract.getCurrentEon(),0)
    }

    fun deposit(alice:CryptoKey,nonce:AtomicInteger,amount: Long){
        chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(123, EtherUtil.Unit.ETHER))

        var ethereumTransaction = EthereumTransaction(
            Address.wrap(contract.getContractAddr()),nonce.getAndIncrement().toLong() , 0,
            0, amount, null)

        chain.newTransaction(alice,ethereumTransaction)

        chain.sb.createBlock()
    }

    @Test
    @ImplicitReflectionSerializer
    fun testDeposit() {

        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        var transactionChannel=chain.watchTransactions({it.tx.from==alice.address&&it.tx.to==Address.wrap(contract.getContractAddr())})
        var amount= EtherUtil.convert(1, EtherUtil.Unit.ETHER).toLong()
        deposit(alice,nonce,amount)

        runBlocking{
            var transaction=transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from,alice.address)
            Assert.assertEquals(transaction.tx.to,Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(),amount)
        }
    }

    @Test
    @ImplicitReflectionSerializer
    @Ignore
    fun testWithDrawal() {
        var nonce = AtomicInteger()
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        var transactionChannel=chain.watchTransactions({it.tx.from==alice.address&&it.tx.to==Address.wrap(contract.getContractAddr())})

        chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(123, EtherUtil.Unit.ETHER))

        var amount= EtherUtil.convert(1, EtherUtil.Unit.ETHER).toLong()
        deposit(alice,nonce,amount)

        runBlocking{
            var transaction=transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from,alice.address)
            Assert.assertEquals(transaction.tx.to,Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(),amount)
        }

        amount  = 100
        val eon = 1
        val path = newPath(alice.address, newUpdate(eon, 1, 0,alice))
        val withdrawal = Withdrawal(alice.address, path, amount)
        var hash=contract.initiateWithdrawal(withdrawal)

        runBlocking{
            var transaction=transactionChannel.receive()
            println(transaction)
        }

    }

    private fun newPath(addr: Address, update: Update): AMTreePath {
        val offset: Long = 100
        val allotment: Long = 1000
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

    private fun newUpdate(eon: Int, version: Long, sendAmount: Long,callUser: CryptoKey): Update {
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
        var transactionChannel=chain.watchTransactions({it.tx.from==alice.address&&it.tx.to==Address.wrap(contract.getContractAddr())})
        var amount= EtherUtil.convert(1, EtherUtil.Unit.ETHER).toLong()
        deposit(alice,nonce,amount)

        runBlocking{
            var transaction=transactionChannel.receive()
            Assert.assertEquals(transaction.tx.from,alice.address)
            Assert.assertEquals(transaction.tx.to,Address.wrap(contract.getContractAddr()))
            Assert.assertEquals(transaction.tx.amount.toLong(),amount)
        }

        var hash=commitHubRoot(0,amount)
        var transaction=chain.findTransaction(hash)
        Assert.assertEquals(transaction?.to,Address.wrap(contract.getContractAddr()))
        Assert.assertEquals(transaction?.from,Address.wrap(chain.sb.sender.address))
    }

    private fun createEon(eon: Int, flag: Boolean,deposit: Long) {
        var ct = 0

        for (i in 0..eon) {
            val tmp = if (flag) {
                (4 * (i + 1))
            } else {
                (4 * (i + 1)) + 2
            }

            var total = (ct - 1) * deposit
            commitHubRoot(i, total)
        }
    }

    private fun commitHubRoot(eon: Int, amount: Long):Hash {
        var height=chain.getNumber()
        if(height?.rem(4)!=0L){
            var blockNumber=4-(height?.rem(4)?:0)
            for(i in 0..blockNumber){
                chain.sb.createBlock()
            }
        }
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0, amount)
        val root = HubRoot(node, eon)
        val callResult = contract.commit(root)
        return callResult
    }

}