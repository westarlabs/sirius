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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        /**
        setResult.receipt.logInfoList.forEach { logInfo ->
            val contract = CallTransaction.Contract(contract.abi)
            val invocation = contract.parseEvent(logInfo)
            println("event:$invocation")
        }*/
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
