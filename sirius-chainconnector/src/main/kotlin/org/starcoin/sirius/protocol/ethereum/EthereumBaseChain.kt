package org.starcoin.sirius.protocol.ethereum

import org.ethereum.config.SystemProperties
import org.ethereum.core.CallTransaction
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ContractConstructArgs
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.contract.EthereumHubContract
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.math.BigInteger

abstract class EthereumBaseChain :
    Chain<EthereumTransaction, EthereumBlock, EthereumAccount> {

    companion object : WithLogging() {

        //TODO
        val defaultGasPrice: BigInteger
            get() = 21000000.toBigInteger()

        val defaultGasLimit: BigInteger
            get() = 8000000.toBigInteger()

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

    override fun loadContract(contractAddress: Address): HubContract<EthereumAccount> {
        TODO("not implemented, load json interface from resource.")
    }

    override fun loadContract(address: Address, jsonInterface: String): EthereumHubContract {
        return EthereumHubContract(address, jsonInterface, this)
    }

    override fun deployContract(account: EthereumAccount, args: ContractConstructArgs): EthereumHubContract {
        return deployContract(account, File(this.javaClass.getResource("/solidity/sirius.sol").toURI()), args)
    }

    override fun deployContract(
        account: EthereumAccount,
        contractFile: File,
        args: ContractConstructArgs
    ): EthereumHubContract {
        val compilationResult = compileContract(contractFile)
        return this.doDeployContract(
            account,
            compilationResult.getContract(contractName),
            args
        )
    }

    fun doDeployContract(
        account: EthereumAccount,
        contractMetadata: CompilationResult.ContractMetadata,
        args: ContractConstructArgs
    ): EthereumHubContract {
        val address = submitNewContract(account, contractMetadata, args.toRLP())
        //TODO wait
        return this.loadContract(address, contractMetadata.abi)
    }


    private fun submitNewContract(
        account: EthereumAccount,
        contractMetaData: CompilationResult.ContractMetadata,
        vararg constructorArgs: Any
    ): Address {
        var contract = CallTransaction.Contract(contractMetaData.abi)
        val constructor: CallTransaction.Function? = contract.constructor
        if (constructor == null && constructorArgs.isNotEmpty()) {
            throw RuntimeException("No constructor with params found")
        }
        val argsEncoded = if (constructor == null) ByteArray(0) else constructor.encodeArguments(*constructorArgs)
        val tx = EthereumTransaction(
            account.getNonce(),
            defaultGasPrice,
            defaultGasLimit,
            contractMetaData.bin.hexToByteArray() + argsEncoded
        )
        this.submitTransaction(account, tx)
        return tx.contractAddress!!
    }

    abstract fun getNonce(address: Address): BigInteger
}
