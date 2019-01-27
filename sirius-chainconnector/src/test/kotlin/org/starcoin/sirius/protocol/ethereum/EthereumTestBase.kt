package org.starcoin.sirius.protocol.ethereum

import org.junit.Assert
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.web3j.crypto.WalletUtils
import java.io.File

open class EthereumServer(var started: Boolean) {
    fun ethStart() {
        if (this.started) return
        scriptExec("clean")
        scriptExec("run")
    }

    fun ethStop() {
        if (this.started) return
        scriptExec("clean")
    }

    companion object {
        private val etherbasePasswd = "starcoinmakeworldbetter"
        private val keystore = "/tmp/geth_data/keystore"
        private val script = "scrpts/docker.sh"
        fun etherbaseKey(): CryptoKey {
            val credentials = WalletUtils.loadCredentials(
                etherbasePasswd,
                File(keystore).let {
                    while (!it.exists() || it.name.contentEquals("tmp")) {
                        Thread.sleep(1000)
                    }; it
                }.listFiles().first()
            )
            return CryptoService.loadCryptoKey(credentials.ecKeyPair.privateKey.toByteArray())
        }
    }

    private fun scriptExec(cmd: String) {
        val process = Runtime.getRuntime().exec("$script $cmd")
        val exit = process.waitFor()
        if (exit != 0) Assert.fail(process.errorStream.bufferedReader().use { it.readText() })
    }
}