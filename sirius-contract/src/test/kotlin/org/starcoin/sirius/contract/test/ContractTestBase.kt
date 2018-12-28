package org.starcoin.sirius.contract.test

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert
import org.junit.Before
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.util.WithLogging
import java.io.File


@Serializable
data class Data(val boolean: Boolean, val int: Int, val string: String, val address: Address) {
    companion object {
        fun random(): Data {
            return random(RandomUtils.nextBoolean())
        }

        fun random(booleanValue: Boolean): Data {
            return Data(
                booleanValue,
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 30)),
                Address.random()
            )
        }
    }
}

abstract class ContractTestBase(val contractFile: String) {

    companion object : WithLogging()

    lateinit var contract: SolidityContract

    @Before
    fun setup() {
        contract = deployContract()
    }

    fun deployContract(): SolidityContract {
        val sb = StandaloneBlockchain().withAutoblock(true)
        val url = this.javaClass::class.java.getResource("/$contractFile")
        val compiler = SolidityCompiler(SystemProperties.getDefault())

        val path = File(url.toURI()).parentFile.absolutePath
        LOG.info("allowed_path:$path")

        val contractName = "TestRLP"
        val compileRes = compiler.compileSrc(
            File(url.toURI()),
            false,
            true,
            SolidityCompiler.Options.ABI,
            SolidityCompiler.Options.BIN,
            SolidityCompiler.Options.AllowPaths(listOf(path))
        )
        Assert.assertFalse("Compile result: " + compileRes.errors, compileRes.isFailed)

        val result = CompilationResult.parse(compileRes.output)
        return sb.submitNewContract(result.getContract(contractName)) as StandaloneBlockchain.SolidityContractImpl
    }
}
