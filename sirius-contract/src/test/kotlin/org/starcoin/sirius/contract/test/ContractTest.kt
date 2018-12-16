package org.starcoin.sirius.contract.test

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.dump
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.serialization.rlp.RLP
import java.io.File
import java.math.BigInteger
import java.net.URL


@Serializable
data class Data(val boolean: Boolean, val byte: Byte, val int: Int, val string: String) {
    companion object {
        fun random(): Data {
            return Data(
                RandomUtils.nextBoolean(),
                RandomUtils.nextInt(0, Byte.MAX_VALUE.toInt()).toByte(),
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 30))
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
            val contract = sb.submitNewContract(result.getContract(contractName))
            val data = Data.random()
            Assert.assertTrue(contract.callFunction("set", RLP.dump(data)).isSuccessful)

            val callResult = contract.callConstFunction("hash")
            Assert.assertTrue(callResult.isNotEmpty())

            Assert.assertEquals(data.boolean, callResult[0] as Boolean)
            //TODO fix
            //Assert.assertEquals(data.byte, (callResult[1] as ByteArray)[0])
            Assert.assertEquals(data.int, (callResult[2] as BigInteger).toInt())
            Assert.assertEquals(data.string, callResult[3] as String)
            //Assert.assertArrayEquals(hash, callResult[0] as ByteArray)
        }
    }
}