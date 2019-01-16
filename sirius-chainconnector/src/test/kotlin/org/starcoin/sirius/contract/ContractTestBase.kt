package org.starcoin.sirius.contract

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.ethereum.config.SystemProperties
import org.ethereum.core.BlockSummary
import org.ethereum.crypto.ECKey
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.solidity.compiler.SolidityCompiler.Options
import org.ethereum.util.blockchain.SolidityCallResult
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert
import org.junit.Before
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.serialization.BigIntegerSerializer
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.MockUtils
import org.starcoin.sirius.util.WithLogging
import java.io.File
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class Data(
    val boolean: Boolean,
    val int: Int,
    val string: String,
    val address: Address, @Serializable(with = BigIntegerSerializer::class) val bigInteger: BigInteger
) {
    companion object : WithLogging() {
        fun random(): Data {
            return random(RandomUtils.nextBoolean())
        }

        fun random(booleanValue: Boolean): Data {
            return Data(
                booleanValue,
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 30)),
                Address.random(),
                MockUtils.nextBigInteger()
            )
        }
    }
}

data class ContractData(val sb: StandaloneBlockchain, val contract: SolidityContract, val owner: ECKey)

abstract class ContractTestBase(val contractFile: String, val contractName: String) {

    companion object : WithLogging() {
        fun bytesToHexString(src: ByteArray?): String? {
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

        fun ethKey2Address(ethKey: EthCryptoKey): Address {
            return Address.getAddress(ethKey.keyPair.public)
        }
    }

    lateinit var sb: StandaloneBlockchain
    lateinit var contract: SolidityContract
    lateinit var callUser: EthCryptoKey
    val blockHeight: AtomicLong = AtomicLong(0)
    lateinit var tx: OffchainTransaction

    @Before
    fun setup() {
        val tmp = deployContract()
        sb = tmp.sb
        contract = tmp.contract
        callUser = EthCryptoKey(tmp.owner)
    }

    open fun getContractConstructArg():Any?{
        return null;
    }

    @Suppress("INACCESSIBLE_TYPE")
    fun deployContract(): ContractData {
        val sb = StandaloneBlockchain().withAutoblock(true).withGasLimit(2147483647).withGasPrice(2147483647)

        val url = this.javaClass::class.java.getResource("$contractFile")
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

        val arg = getContractConstructArg()
        val contract = (arg?.let{sb.submitNewContract(contractMetadata, arg)} ?: sb.submitNewContract(contractMetadata)) as StandaloneBlockchain.SolidityContractImpl

        val lastSummary = StandaloneBlockchain::class.java.getDeclaredField("lastSummary")
        lastSummary.setAccessible(true)
        val sum = lastSummary.get(sb) as BlockSummary
        sum.getReceipts().stream().forEach { receipt -> assert(receipt.isTxStatusOK) }

        sb.addEthereumListener(object : EthereumListenerAdapter() {
            override fun onBlock(blockSummary: BlockSummary) {
                blockHeight.incrementAndGet()
                LOG.info("block length:$blockHeight")
            }
        })

        return ContractData(sb, contract, sb.sender)
    }

    fun call(data: ByteArray, method: String, hasReturn: Boolean) {
        val dataStr = bytesToHexString(data)!!

        LOG.info("contract args:$dataStr")

        val callResult = contract.callFunction(method, data)

        LOG.warning("conract err:$callResult.receipt.error")

        assert(callResult.receipt.isTxStatusOK)
        if (hasReturn) {
            val resultStr = bytesToHexString(callResult.receipt.executionResult)!!

            LOG.info("contract return:$resultStr")
        } else {
            callResult.receipt.logInfoList.forEach { logInfo ->
                LOG.info("event:$logInfo")
            }
        }
    }

//    fun commitData(eon: Int, amount: Long, flag: Boolean) {
//        commitData(this.contract, eon, amount, flag)
//    }

    fun commitData(eon: Int, amount: Long, flag: Boolean) {
        val info = AMTreeInternalNodeInfo(Hash.random(), amount, Hash.random())
        val node = AMTreePathInternalNode(info, PathDirection.ROOT, 0, amount)
        val root = HubRoot(node, eon)
        val data = RLP.dump(HubRoot.serializer(), root)
        val callResult = contract.callFunction("commit", data)


        if (flag) {
            assert(callResult.returnValue as Boolean)
            verifyReturn(callResult)
        } else {
            LOG.warning(callResult.receipt.error)
            callResult.receipt.logInfoList.forEach { logInfo ->
                LOG.info("event:$logInfo")
            }
        }
    }

    fun commitRealData(eon: Int, up: Update,  allotment : Long,  amount: Long, flag: Boolean, txs:MutableList<OffchainTransaction>) {
        val accounts = mutableListOf<HubAccount>()
        val realEon = eon + 1
        accounts.add(HubAccount(callUser.keyPair.public, up, allotment, amount, 0, txs))
        val tree = AMTree(realEon, accounts)
        val node = tree.root.toAMTreePathNode() as AMTreePathInternalNode

        val root = HubRoot(node, realEon)
        val data = RLP.dump(HubRoot.serializer(), root)
        val callResult = contract.callFunction("commit", data)


        if (flag) {
            assert(callResult.returnValue as Boolean)
            verifyReturn(callResult)
        } else {
            LOG.warning(callResult.receipt.error)
            callResult.receipt.logInfoList.forEach { logInfo ->
                LOG.info("event:$logInfo")
            }
        }
    }

    fun verifyReturn(callResult: SolidityCallResult) {
        LOG.warning(callResult.receipt.error)
        Assert.assertTrue(callResult.isSuccessful)
        callResult.receipt.logInfoList.forEach { logInfo ->
            LOG.info("event:$logInfo")
        }
    }
}
