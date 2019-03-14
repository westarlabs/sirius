package org.starcoin.sirius.wallet.command

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.wallet.core.Wallet
import picocli.CommandLine
import java.lang.Exception
import java.math.BigInteger

@CommandLine.Command(
    name = "wallet",
    description = arrayOf("manage wallet"),
    mixinStandardHelpOptions = true,
    subcommands = arrayOf(
        NewTransfer::class,
        Deposit::class,
        OpenTransferDeliveryChallenge::class,
        WithDrawal::class,
        GetHubAccount::class,
        Register::class,
        LocalBalance::class,
        CheatMode::class,
        RecieveTransaction::class,
        RecieveHubSign::class,
        HubInfo::class,
        ChainTransfer::class,
        HubStatus::class
    ))
class WalletCommand<T : ChainTransaction, A : ChainAccount>(internal var wallet: Wallet<T, A>) : Runnable {

    override fun run() {
    }
}

@CommandLine.Command(name = "deposit", description = ["deposit to hub account"])
class Deposit<T : ChainTransaction, A : ChainAccount>: Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["value"], description = ["coin amount"], required = true)
    var value: Long = 0

    override fun run() {
        try {
            val succResponse = walletCommand!!.wallet.deposit(BigInteger.valueOf(value))
            println(succResponse)
        } catch (e: Throwable) {
            System.out.println(e.getLocalizedMessage())
        }
    }
}

@CommandLine.Command(name = "new_transfer", description = ["get unspent transaction output for current wallet address"])
class NewTransfer<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["addr"], description = ["destination address"], required = true)
    var addr: Address? = null

    @CommandLine.Option(names = ["value"], description = ["coin amount"], required = true)
    var value: BigInteger = BigInteger.ZERO

    override fun run() {
        try {
            val tx = walletCommand?.wallet?.hubTransfer(addr!!, value)
            System.out.println("transaction hash is :" + tx!!.hash().toMD5Hex())
        } catch (e: Exception) {
            System.out.println(e.getLocalizedMessage())
        }

    }

}

@CommandLine.Command(name = "reg", description = ["regisger hub account"])
class Register<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            val updateResponse = walletCommand!!.wallet.register()
            System.out.println(updateResponse)
        } catch (e: StatusRuntimeException) {
            if (e.status == Status.ALREADY_EXISTS) {
                println("user exists,you may need sync status from hub")
            }
        }

    }
}

@CommandLine.Command(name = "account", description = ["get hub account"])
class GetHubAccount<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        val hubAccount = walletCommand!!.wallet.hubAccount()
        if (hubAccount != null) {
            System.out.println(hubAccount.toString())
            System.out.println(hubAccount.address.toBytes().toHEXString())
        }
    }
}

@CommandLine.Command(name = "otdc", description = ["open transfer delivery challenge"])
class OpenTransferDeliveryChallenge<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["txh"], description = ["hash"], required = true)
    var transactionHash: String? = null

    override fun run() {
        try {
            val succResponse = walletCommand!!.wallet.openTransferChallenge(Hash.wrap(transactionHash!!))
            println(succResponse)
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "wd", description = ["withdrawal"])
class WithDrawal<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["value"], description = ["coin amount"], required = true)
    var value: Long = 0

    override fun run() {
        try {
            val txDeferred = walletCommand!!.wallet.withdrawal(BigInteger.valueOf(value))
            println(txDeferred)
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "lb", description = ["local balance"])
class LocalBalance<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            System.out.println("hub balance is " + walletCommand!!.wallet.hub.getAvailableCoin())
            System.out.println("withdrawal coin is " + walletCommand!!.wallet.hub.getWithdrawalCoin())
            System.out.println("chain balance is " + walletCommand!!.wallet.blockChain.chainBalance())
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "sync", description = ["sync hub status"])
class SyncHub<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        walletCommand!!.wallet.sync()
        println("sync finish")
    }
}

@CommandLine.Command(name = "cheat", description = ["set cheat mode flag"])
class CheatMode<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["flag"], description = ["flag"], required = true)
    var flag: Int = 0

    override fun run() {
        try {
            val flags = walletCommand!!.wallet.cheat(flag)
            System.out.println(flags)
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "rt", description = ["recieve transacton"])
class RecieveTransaction<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            walletCommand!!.wallet.recieveTransacion()
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "rhs", description = ["recieve hub signed update"])
class RecieveHubSign<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            walletCommand!!.wallet.recieveHubSign()
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "hub_info", description = ["query hub info"])
class HubInfo<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            println(walletCommand!!.wallet.hubInfo())
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "restore", description = ["restore wallet from db"])
class Restore<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            walletCommand!!.wallet.restore()
        } catch (e: Exception) {
            System.out.println(e.message)
        }

    }
}

@CommandLine.Command(name = "chain_transfer", description = ["transfer on chain"])
class ChainTransfer<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    @CommandLine.Option(names = ["addr"], description = ["destination address"], required = true)
    var addr: Address? = null

    @CommandLine.Option(names = ["value"], description = ["coin amount"], required = true)
    var value: BigInteger = BigInteger.ZERO

    override fun run() {
        try {
            val tx = walletCommand!!.wallet.chainTransfer(addr!!, value)
            System.out.println("transaction hash is :" + tx.hash())
        } catch (e: Exception) {
            System.out.println(e.getLocalizedMessage())
        }

    }
}

@CommandLine.Command(name = "hubstatus", description = ["hub status"])
class HubStatus<T : ChainTransaction, A : ChainAccount> : Runnable {

    @CommandLine.ParentCommand
    var walletCommand: WalletCommand<T,A>? = null

    override fun run() {
        try {
            val result = walletCommand!!.wallet.isRecoveryMode()
            System.out.println("hub in in recovery mode is $result" )
        } catch (e: Exception) {
            System.out.println(e.getLocalizedMessage())
        }

    }
}
