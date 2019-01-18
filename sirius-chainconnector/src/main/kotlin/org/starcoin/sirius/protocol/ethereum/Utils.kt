package org.starcoin.sirius.protocol.ethereum

import org.ethereum.solidity.compiler.CompilationResult
import org.starcoin.sirius.lang.toClassPathResource

fun loadContractMetadata(contractPath: String = "solidity/SiriusService"): CompilationResult.ContractMetadata {
    val contractMetadata = CompilationResult.ContractMetadata()//result.getContract(contractName)
    contractMetadata.abi = "$contractPath.abi".toClassPathResource().readAsText()
    contractMetadata.bin = "$contractPath.bin".toClassPathResource().readAsText()
    return contractMetadata
}
