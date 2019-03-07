package org.starcoin.sirius.protocol.ethereum.contract

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import org.ethereum.core.CallTransaction
import org.ethereum.solidity.SolidityType
import org.ethereum.vm.LogInfo
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.ContractReturn
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.core.fail
import org.starcoin.sirius.protocol.ContractFunction
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.protocol.TxDeferred
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.EthereumBaseChain
import org.starcoin.sirius.serialization.rlp.RLP
import org.starcoin.sirius.util.WithLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class EthereumHubContract internal constructor(
    override val contractAddress: Address,
    jsonInterface: String,
    val chain: EthereumBaseChain
) : HubContract<EthereumAccount>() {

    companion object : WithLogging()

    private val contract: CallTransaction.Contract = CallTransaction.Contract(jsonInterface)


    override fun <S : SiriusObject> executeContractFunction(
        account: EthereumAccount,
        function: ContractFunction<S>,
        arguments: S
    ): TxDeferred {
        val data = function.encode(arguments)
        val hash = chain.submitTransaction(
            account,
            EthereumTransaction(
                this.contractAddress, account.getNonce(),
                //TODO gas calculate
                EthereumBaseChain.defaultGasPrice, EthereumBaseChain.defaultGasLimit, data
            )
        )
        LOG.info("executeContractFunction ${function.name}, txHash:$hash, input:$arguments")
        return hash
    }

    val bytesType: SolidityType.BytesType = SolidityType.BytesType()

    @Suppress("UNCHECKED_CAST")
    @UseExperimental(ImplicitReflectionSerializer::class)
    override fun <S : Any> queryContractFunction(
        account: EthereumAccount,
        functionName: String,
        clazz: KClass<S>,
        vararg args: Any
    ): S? {
        val function = this.contract.getByName(functionName)
            ?: fail { "Can not find function by name:$functionName" }
        val data = function.encode(*args)
        val returnBytes = this.chain.callConstFunction(account.key, this.contractAddress, data)
        val result = function.decodeResult(returnBytes)[0]
        return if (clazz.isSubclassOf(SiriusObject::class)) {
            val bytes =
                bytesType.decode(returnBytes, SolidityType.IntType.decodeInt(returnBytes, 0).toInt()) as ByteArray
            if (bytes.isEmpty()) {
                return null
            }
            val contractReturn = RLP.load(ContractReturn.serializer(), bytes)
            contractReturn.getPayload(clazz as KClass<SiriusObject>) as S?
        } else {
            result as S
        }
    }

    fun callFunction(account: EthereumAccount, functionName: String, vararg args: Any): TxDeferred {
        val function = this.contract.getByName(functionName)
        val data = function.encode(*args)
        return chain.submitTransaction(
            account,
            EthereumTransaction(
                this.contractAddress, account.getNonce(),
                //TODO
                EthereumBaseChain.defaultGasPrice, EthereumBaseChain.defaultGasLimit, data
            )
        )
    }

    override fun setHubIp(account: EthereumAccount, ip: String) {
        runBlocking { callFunction(account, "hubIp", ip.toByteArray()).await() }
    }

    fun parseEvent(eventLog: LogInfo): CallTransaction.Invocation {
        return this.contract.parseEvent(eventLog)
    }
}
