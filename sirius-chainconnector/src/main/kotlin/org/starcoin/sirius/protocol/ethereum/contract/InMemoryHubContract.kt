package org.starcoin.sirius.protocol.ethereum.contract

import org.ethereum.core.CallTransaction
import org.ethereum.crypto.ECKey
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.starcoin.sirius.core.*
import org.starcoin.sirius.protocol.HubContract
import org.starcoin.sirius.serialization.rlp.RLP
import java.math.BigInteger

class InMemoryHubContract(contract: SolidityContract,owner : ECKey) : HubContract{

    internal var contract =contract

    internal var owner = owner

    override fun getContractAddr():ByteArray{
        return contract.address
    }

    fun hubIp(ip:String){
        val callResult = contract.callFunction("hubIp", ip.toByteArray())
    }

    override fun queryHubInfo(): ContractHubInfo {
        var res=contract.callConstFunction("hubInfo")
        return ContractHubInfo.parseFromRLP(res[0] as ByteArray)
    }

    override fun queryLeastHubCommit(): HubRoot {
        val callResult = contract.callConstFunction("getLatestRoot")
        return HubRoot.parseFromRLP(callResult[0] as ByteArray)
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
        val withdrawalData = RLP.dump(Withdrawal.serializer(), request)
        val setResult = contract.callFunction("initiateWithdrawal", withdrawalData)
        setResult.receipt.logInfoList.forEach { logInfo ->
            val contract = CallTransaction.Contract(contract.abi)
            val invocation = contract.parseEvent(logInfo)
            println("event:$invocation")
        }
        if(setResult.isSuccessful)
            return Hash.of(setResult.receipt.transaction.hash)
        else
            return Hash.ZERO_HASH
    }

    override fun cancelWithdrawal(request: CancelWithdrawal): Hash {
        return callContract("cancelWithdrawal",RLP.dump(CancelWithdrawal.serializer(),request))
    }

    override fun openBalanceUpdateChallenge(request: BalanceUpdateChallenge): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeBalanceUpdateChallenge(request: BalanceUpdateProof): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun commit(request: HubRoot): Hash {
        return callContract("commit",RLP.dump(HubRoot.serializer(), request))
    }

    private fun  callContract(funcName :String,data :ByteArray):Hash{
        val setResult = contract.callFunction(funcName, data)
        setResult.receipt.logInfoList.forEach { logInfo ->
            val contract = CallTransaction.Contract(contract.abi)
            val invocation = contract.parseEvent(logInfo)
            println("event:$invocation")
        }
        assert(setResult.returnValue as Boolean)
        if(setResult.isSuccessful)
            return Hash.of(setResult.receipt.transaction.hash)
        else
            return Hash.ZERO_HASH
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

    fun getCurrentEon():Int{
        val eonObject=this.contract.callFunction("getCurrentEon") as StandaloneBlockchain.SolidityCallResultImpl
        return (eonObject.returnValue as BigInteger).intValueExact()
    }
}
