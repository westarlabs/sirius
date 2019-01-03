package org.starcoin.sirius.protocol.ethereum.contract

import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.config.SystemProperties
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import java.io.File
import java.math.BigInteger
import kotlin.properties.Delegates


class EthereumHubContract(private val chain: EthereumChain) : HubContract {
    val contractName = "SiriusService"
    private val compiler = SolidityCompiler(SystemProperties.getDefault())
    private val contract: Address by Delegates.notNull()

    class ContractFun(val name: String) {
        fun setFunArgs(vararg args: String) {
        }

        fun encode() {
        }
    }

    fun compileContract(srcName: String = "solidity/sirius.sol"): CompilationResult {
        val srcPath = javaClass::class.java.getResource(srcName).toURI()
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

    fun deployContract(
        contract: CompilationResult,
        key: CryptoKey,
        gasPrice: Long,
        gasLimit: Long,
        value: Long = 0
    ): Address {
        val tx = EthereumTransaction(
            null, chain.getNonce(key.address),
            gasPrice, gasLimit, value,
            contract.getContract(contractName).bin.toByteArray()
        )
        this.chain.newTransaction(key, tx)
        return Address.wrap(tx.ethTx.contractAddress)
    }

    fun deployContract(
        srcName: String = "solidity/sirius.sol",
        key: CryptoKey,
        gasPrice: Long,
        gasLimit: Long,
        value: Long = 0
    ): Address {
        return deployContract(compileContract(srcName), key, gasPrice, gasLimit, value)
    }

    fun callFun(caller: CryptoKey, contract: Address) {

    }


    fun callConstFun() {

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