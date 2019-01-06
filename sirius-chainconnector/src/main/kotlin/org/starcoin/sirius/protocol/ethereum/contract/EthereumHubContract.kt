package org.starcoin.sirius.protocol.ethereum.contract

import org.ethereum.config.SystemProperties
import org.ethereum.core.CallTransaction.Contract
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import org.starcoin.sirius.util.Utils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.io.File
import kotlin.properties.Delegates

class EthereumHubContract private constructor(
    val chain: EthereumChain
) : HubContract {
    private val contractName = "SiriusService"
    private var contractAddress: Address by Delegates.notNull()
    private var contract: Contract by Delegates.notNull()
    private var contractResult: CompilationResult by Delegates.notNull()

    constructor(
        chain: EthereumChain,
        srcName: String = "solidity/sirius.sol",
        contractAddress: Address
    ) : this(chain) {
        this.contractAddress = contractAddress
        this.contractResult = compileContract(srcName)
        this.contract = Contract(contractResult.getContract(contractName).abi)
    }

    constructor(
        chain: EthereumChain,
        srcName: String = "solidity/sirius.sol",
        key: CryptoKey,
        gasPrice: Long,
        gasLimit: Long,
        value: Long = 0
    ):this(chain) {
        this.contractAddress = submitContract(key, gasPrice, gasLimit, value)
        this.contractResult = compileContract(srcName)
        this.contract = Contract(contractResult.getContract(contractName).abi)
    }

    companion object {
        private val compiler = SolidityCompiler(SystemProperties.getDefault())
        fun compileContract(srcName: String): CompilationResult {
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
    }

    fun submitContract(
        key: CryptoKey,
        gasPrice: Long,
        gasLimit: Long,
        value: Long = 0
    ): Address {
        val tx = EthereumTransaction(
            null, chain.getNonce(key.address),
            gasPrice, gasLimit, value,
            contractResult.getContract(contractName).bin.toByteArray()
        )
        chain.newTransaction(key, tx)
        return Address.wrap(tx.ethTx.contractAddress)
    }


    fun callFunction(
        caller: CryptoKey,
        gasPrice: Long,
        gasLimit: Long,
        value: Long,
        name: String,
        vararg args: Any
    ) {
        val function = this.contract.getByName(name)
        val data = function.encode(args)
        chain.newTransaction(
            caller,
            EthereumTransaction(
                this.contractAddress, this.chain.getNonce(caller.address),
                gasPrice, gasLimit, value, data)
        )
    }

    fun callConstFunction(caller:CryptoKey,name: String,vararg args:Any) :String{
        val function = this.contract.getByName(name)
        val data = function.encode(args)
        val resp = this.chain.web3.ethCall(
            Transaction.createEthCallTransaction(caller.address.toString(),
                this.contractAddress.toString(),
                Utils.HEX.encode(data)
            ), DefaultBlockParameterName.LATEST).sendAsync().get()
        if (resp.hasError()) throw RuntimeException(resp.error.message)
        return resp.value
    }

    override fun queryHubInfo(): ContractHubInfo {
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
