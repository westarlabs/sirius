package org.starcoin.sirius.protocol.ethereum.contract

import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.config.SystemProperties
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import java.io.File
import java.math.BigInteger
import java.net.URL
import java.security.KeyPair


class EthereumHubContract(private val chain: EthereumChain) : HubContract {

    private val compiler = SolidityCompiler(SystemProperties.getDefault())
    val contractName = "SiriusService"

    fun compileContract(sourceName: String = "solidity/sirius.sol"): CompilationResult {
        val srcPath = javaClass::class.java.getResource(sourceName).toURI()
        val srcDir = listOf(File(srcPath).parentFile.absolutePath)
        val compiledResult = compiler.compileSrc(
            File(srcPath),
            true,
            true,
            SolidityCompiler.Options.ABI,
            SolidityCompiler.Options.BIN,
            SolidityCompiler.Options.AllowPaths(srcDir)
        )
        if (compiledResult.isFailed) throw RuntimeException("Compile result: " + compiledResult.errors)
        return CompilationResult.parse(compiledResult.output)
    }

    fun newContract(compiledContract: CompilationResult, key: CryptoKey, GasLimit: BigInteger, Gas: BigInteger) {
    }

    override fun queryHubInfo(): HubInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryLeastHubCommit(): HubRoot {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryHubCommit(eon: Int): HubRoot {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryCurrentBalanceUpdateChallenge(address: Address): BalanceUpdateChallenge {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryCurrentTransferDeliveryChallenge(address: Address): TransferDeliveryChallenge {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryWithdrawalStatus(address: Address): WithdrawalStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initiateWithdrawal(request: Withdrawal): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancelWithdrawal(request: CancelWithdrawal): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeBalanceUpdateChallenge(request: BalanceUpdateProof): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun commit(request: HubRoot): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openTransferDeliveryChallenge(request: TransferDeliveryChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeTransferDeliveryChallenge(request: CloseTransferDeliveryChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recoverFunds(request: AMTreeProof): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContractAddr(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}