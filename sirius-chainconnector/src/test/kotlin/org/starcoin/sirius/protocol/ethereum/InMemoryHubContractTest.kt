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
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.EventTopic
import org.starcoin.sirius.protocol.ethereum.contract.InMemoryHubContract
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
        contract = InMemoryHubContract(chain.sb.submitNewContract(result.getContract(contractName)))

    }

    @Test
    @ImplicitReflectionSerializer
    fun testCurrentEon(){
        Assert.assertEquals(contract.getCurrentEon(),0)
    }

    @Test
    @ImplicitReflectionSerializer
    fun testDeposit() {

        var nonce = AtomicInteger();
        var alice = CryptoService.generateCryptoKey()

        //var transactions = List<EthereumTransaction>
        chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(123, EtherUtil.Unit.ETHER))

        var ethereumTransaction = EthereumTransaction(
            Address.wrap(contract.getContractAddr()),nonce.getAndIncrement().toLong() , 0,
            0, 1, null)

        chain.newTransaction(alice,ethereumTransaction)

        println(ethereumTransaction.ethTx.key)
        chain.sb.createBlock()

        var blockChannel=chain.watchBlock(Address.wrap(contract.getContractAddr()),EventTopic.Deposit)

        runBlocking{
            var block=blockChannel.receive()
            Assert.assertEquals(block.getTransactions().size,1)
        }
    }
}