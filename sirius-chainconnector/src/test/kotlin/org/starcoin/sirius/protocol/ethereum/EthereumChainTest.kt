package org.starcoin.sirius.protocol.ethereum

import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

class EthereumChainTest {
    @Test
    fun testfindTransaction(){
        
    }

    @Test
    fun testGetBlock(){
        
    }

    @Test
    fun testBlockWatch() {
        val web3j = Web3j.build(HttpService("http://39.96.66.145:8545"))
        val blockReq = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(10)), true).send()
        println("The block info is ${blockReq.block.nonce}, ${blockReq.block.transactions.size}")
    }
}