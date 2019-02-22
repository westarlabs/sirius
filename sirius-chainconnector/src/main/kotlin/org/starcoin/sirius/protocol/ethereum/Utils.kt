package org.starcoin.sirius.protocol.ethereum

import org.ethereum.solidity.compiler.CompilationResult
import org.junit.Assert
import org.starcoin.sirius.lang.toClassPathResource
import java.io.File

fun loadContractMetadata(contractPath: String = "solidity/SiriusService"): CompilationResult.ContractMetadata {
    val contractMetadata = CompilationResult.ContractMetadata()//result.getContract(contractName)
    contractMetadata.abi = "$contractPath.abi".toClassPathResource().readAsText()
    contractMetadata.bin = "$contractPath.bin".toClassPathResource().readAsText()
    return contractMetadata
}

fun loadEtherBaseKeyStoreFile(keystore: String): File {
    return File(keystore).let {
        while (!it.exists() || it.name.contentEquals("tmp")) {
            Thread.sleep(1000)
        }; it
    }.listFiles().first()
}

fun scriptExec(cmd: String): Int {
    val process = Runtime.getRuntime().exec("$cmd")
    val exit = process.waitFor()
    if (exit != 0)
        throw RuntimeException(process.errorStream.bufferedReader().use { it.readText() })
    return exit
}

