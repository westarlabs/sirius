package org.starcoin.sirius.protocol.ethereum.contract

import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.config.SystemProperties
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.ethereum.EthereumChain
import java.io.File
import java.net.URL
import java.security.KeyPair


class EthereumHubContract(chain: EthereumChain) : HubContract {

    private val chain: EthereumChain = chain
    private val contract: ByteArray = ByteArray(0)
    private val compiler = SolidityCompiler(SystemProperties.getDefault())
    fun loadResource(name: String): URL {
        return javaClass::class.java.getResource(name) ?: File("./out/test/resources/$name").toURL()
    }
    val contractName = "SiriusService"
    fun newContract(
        keyPair: KeyPair,
        to: Address,
        nonce: Long,
        gasPrice: Long,
        gasLimit: Long,
        value: Long
    ) {
        val contractCode: ByteArray = ByteArray(0)
        val solResource = javaClass::class.java.getResource("solidity/sirius.sol")
            ?: File("./out/test/resources/solidity/sirius.sol").toURL()
        val solUri = solResource.toURI()
        val compiledResult = compiler.compileSrc(
            File(solUri),
            false,
            true,
            SolidityCompiler.Options.ABI,
            SolidityCompiler.Options.BIN,
            SolidityCompiler.Options.AllowPaths(listOf(File(solUri).parentFile.absolutePath))
        )
        if (compiledResult.isFailed) throw RuntimeException("Compile result: " + compiledResult.errors)
        val result = CompilationResult.parse(compiledResult.output)
        var con = result.getContract(contractName)
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

    override fun recoverFunds(request: Starcoin.RecoverFundsRequest): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



}