package org.starcoin.sirius.protocol.ethereum

import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.protocol.ethereum.contract.InMemoryHubContract
import java.io.File
import kotlin.properties.Delegates

class InMemoryHubContractTest {

    private var chain : InMemoryChain by Delegates.notNull()
    private var contract : InMemoryHubContract by Delegates.notNull()

    @Before
    fun beforeTest(){
        chain = InMemoryChain(true)
        val compiler = SolidityCompiler(SystemProperties.getDefault())

        val solRResource= this.javaClass::class.java.getResource("/solidity/sirius.sol")
        val solUri = solRResource.toURI()

        val path = File(solUri).parentFile.absolutePath
        //println("allowed_path:$path")

        val contractName = "SiriusService"
        val compileRes = compiler.compileSrc(
            File(solUri),
            false,
            true,
            SolidityCompiler.Options.ABI,
            SolidityCompiler.Options.BIN,
            SolidityCompiler.Options.AllowPaths(listOf(path))
        )
        if (compileRes.isFailed()) throw RuntimeException("Compile result: " + compileRes.errors)

        val result = CompilationResult.parse(compileRes.output)

        var con= result.getContract(contractName)
        //println(con.bin)
        con.bin=con.bin.replace("_","").replace("$","")
        contract = InMemoryHubContract(chain.sb.submitNewContract(result.getContract(contractName)))

    }

    @Test
    @ImplicitReflectionSerializer
    fun testCurrentEon(){
        println(contract.getCurrentEon())
    }
}