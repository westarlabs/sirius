package org.starcoin.sirius.contract.test

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.dump
import kotlinx.serialization.load
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.ethereum.config.SystemProperties
import org.ethereum.core.CallTransaction
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.serialization.rlp.RLP
import java.io.File
import java.net.URL


@Serializable
data class Data(val boolean: Boolean, val int: Int, val string: String, val address: Address) {
    companion object {
        fun random(): Data {
            return Data(
                //TODO test for true and false
                RandomUtils.nextBoolean(),
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 30)),
                Address.random()
            )
        }
    }
}

class ContractTest {

    fun String.asResource(work: (URL) -> Unit) {
        val url = this.javaClass::class.java.getResource(this)
        work(url)
    }

    @ImplicitReflectionSerializer
    @Test
    fun testRLP() {
        val sb = StandaloneBlockchain().withAutoblock(true)

        val compiler = SolidityCompiler(SystemProperties.getDefault())
        "/TestRLP.sol".asResource {

            val path = File(it.toURI()).parentFile.absolutePath
            //val path = "."
            println("allowed_path:$path")

            val contractName = "TestRLP"
            val compileRes = compiler.compileSrc(
                File(it.toURI()),
                false,
                true,
                SolidityCompiler.Options.ABI,
                SolidityCompiler.Options.BIN,
                SolidityCompiler.Options.AllowPaths(listOf(path))
            )
            if (compileRes.isFailed()) throw RuntimeException("Compile result: " + compileRes.errors)

            val result = CompilationResult.parse(compileRes.output)
            val contract =
                sb.submitNewContract(result.getContract(contractName)) as StandaloneBlockchain.SolidityContractImpl
            val data = Data.random()
            println("data:$data")
            val dataRLP = RLP.dump(data)
            val setResult = contract.callFunction("set", dataRLP)
            setResult.receipt.logInfoList.forEach { logInfo ->
                //val eventData = RLP.load<Data>(logInfo.data)
                //Assert.assertEquals(data, eventData)
                val contract = CallTransaction.Contract(contract.abi)
                val invocation = contract.parseEvent(logInfo)
                println("event:$invocation")
            }
            println("error:${setResult.receipt.error}")
            Assert.assertTrue(setResult.isSuccessful)


//            val callEchoByteResult = contract.callConstFunction("echoByte", 1.toByte())
//            Assert.assertEquals(callEchoByteResult[0], 1.toByte())

            val callResult = contract.callConstFunction("echo", dataRLP)
            //val callResult = contract.callConstFunction("get")
            Assert.assertTrue(callResult.isNotEmpty())
            val returnDataRLP = callResult[0] as ByteArray
            Assert.assertArrayEquals(dataRLP, returnDataRLP)
            val returnData = RLP.load<Data>(returnDataRLP)
            Assert.assertEquals(data, returnData)
            //Assert.assertEquals(data.boolean, callResult[0] as Boolean)
            //TODO fix
            //Assert.assertEquals(data.byte, (callResult[1] as ByteArray)[0])
            //Assert.assertEquals(data.int, (callResult[2] as BigInteger).toInt())
            //Assert.assertEquals(data.string, callResult[3] as String)
            //Assert.assertArrayEquals(hash, callResult[0] as ByteArray)
        }
    }
}
