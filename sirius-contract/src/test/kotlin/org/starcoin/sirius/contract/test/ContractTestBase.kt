package org.starcoin.sirius.contract.test

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.ethereum.config.SystemProperties
import org.ethereum.core.*
import org.ethereum.crypto.ECKey
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.solidity.compiler.SolidityCompiler.Options
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert
import org.junit.Before
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.crypto.fallback.DefaultCryptoKey
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.security.KeyPair
import java.security.PrivateKey
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class Data(val boolean: Boolean, val int: Int, val string: String, val address: Address) {
    companion object : WithLogging() {
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

data class ContractData(val sb: StandaloneBlockchain, val contract: SolidityContract, val owner: ECKey)

abstract class ContractTestBase(val contractFile: String, val contractName: String) {

    companion object : WithLogging()

    lateinit var sb: StandaloneBlockchain
    lateinit var contract: SolidityContract
    lateinit var owner: KeyPair
    lateinit var addr: Address
    val blockHeight: AtomicLong = AtomicLong(0)

    @Before
    fun setup() {
        val tmp = deployContract()
        sb = tmp.sb
        contract = tmp.contract

        val privKey = DefaultCryptoKey.generatePrivateKeyFromBigInteger(tmp.owner.privKey)
        owner = KeyPair(
            DefaultCryptoKey.generatePublicKeyFromPrivateKey(privKey), privKey
        )

        addr = Address.getAddress(owner.public)
    }

    @Suppress("INACCESSIBLE_TYPE")
    fun deployContract(): ContractData {
        val sb = StandaloneBlockchain().withAutoblock(true).withGasLimit(2147483647)

        val url = this.javaClass::class.java.getResource("/$contractFile")
        val compiler = SolidityCompiler(SystemProperties.getDefault())

        val path = File(url.toURI()).parentFile.absolutePath
        LOG.info("allowed_path:$path")
        val compileRes = compiler.compileSrc(
            File(url.toURI()),
            true,
            true,
            Options.ABI,
            Options.BIN,
            Options.AllowPaths(listOf(path))
        )
        Assert.assertFalse("Compile result: " + compileRes.errors, compileRes.isFailed)

        val result = CompilationResult.parse(compileRes.output)
        val contractMetadata = result.getContract(contractName)
        LOG.info("$contractFile compile abi ${contractMetadata.abi}")
        LOG.info("$contractFile compile bin ${contractMetadata.bin}")
        val contract = sb.submitNewContract(contractMetadata) as StandaloneBlockchain.SolidityContractImpl

        val lastSummary = StandaloneBlockchain::class.java.getDeclaredField("lastSummary")
        lastSummary.setAccessible(true)
        val sum = lastSummary.get(sb) as BlockSummary
        sum.getReceipts().stream().forEach { receipt -> println(receipt.error + ":" + receipt.isTxStatusOK) }


        sb.addEthereumListener(object : EthereumListenerAdapter() {
            override fun onBlock(blockSummary: BlockSummary) {
                blockHeight.incrementAndGet()
                println(blockHeight.get())
            }
        })

        return ContractData(sb, contract, sb.sender)
    }

    fun call(data: ByteArray, method: String) {
        call(data, method, true)
    }

    fun call(data: ByteArray, method: String, hasReturn: Boolean) {
        val dataStr = bytesToHexString(data)!!

        println(dataStr)

        val callResult = contract.callFunction(method, data)

        println(callResult.receipt.error)

        assert(callResult.receipt.isTxStatusOK)
        if (hasReturn) {
            val resultStr = bytesToHexString(callResult.receipt.executionResult)!!

            println(resultStr)
        } else {
            callResult.receipt.logInfoList.forEach { logInfo ->
                println("event:$logInfo")
            }
        }
    }

    private fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.isEmpty()) {
            return null
        }
        for (i in 0..src.size - 1) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }
}
