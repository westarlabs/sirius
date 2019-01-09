package org.starcoin.sirius.protocol.ethereum

import org.ethereum.config.SystemProperties
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.contract.EthereumHubContract
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.math.BigInteger

abstract class EthereumBaseChain :
    Chain<EthereumTransaction, EthereumBlock, EthereumAccount> {

    companion object : WithLogging() {

        //TODO
        val defaultGasPrice: BigInteger
            get() = 210000.toBigInteger()

        val defaultGasLimit: BigInteger
            get() = 2100000.toBigInteger()

        val contractName: String = "SiriusService"

        private val compiler = SolidityCompiler(SystemProperties.getDefault())
        fun compileContract(contractFile: File): CompilationResult {
            val srcDir = listOf(contractFile.parentFile.absolutePath)
            val compiledResult = compiler.compileSrc(
                contractFile,
                true,
                true,
                SolidityCompiler.Options.ABI,
                SolidityCompiler.Options.BIN,
                SolidityCompiler.Options.AllowPaths(srcDir)
            )
            LOG.info("compile ${contractFile.absolutePath} results: ${compiledResult.output}")
            if (compiledResult.isFailed) throw RuntimeException("Compile result: " + compiledResult.errors)
            return CompilationResult.parse(compiledResult.output)
        }
    }

    abstract fun callConstFunction(caller: CryptoKey, contractAddress: Address, data: ByteArray): ByteArray

    override fun loadContract(address: Address, jsonInterface: String): EthereumHubContract {
        return EthereumHubContract(address, jsonInterface, this)
    }

    override fun deployContract(account: EthereumAccount): EthereumHubContract {
        return deployContract(account, File(this.javaClass.getResource("/solidity/sirius.sol").toURI()))
    }

    override fun deployContract(account: EthereumAccount, contractFile: File): EthereumHubContract {
        val compilationResult = compileContract(contractFile)
        return this.doDeployContract(
            account,
            compilationResult.getContract(contractName)
        )
    }

    abstract fun doDeployContract(
        account: EthereumAccount,
        contractMetadata: CompilationResult.ContractMetadata
    ): EthereumHubContract

    abstract fun getNonce(address: Address): BigInteger
}
