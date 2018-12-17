package org.starcoin.sirius.protocol.ethereum

import org.junit.Test

class EthereumChainTest {
    @Test
    fun testfindTransaction(){
        
    }

    @Test
    fun testGetBlock(){
        
    }

    @Test
    fun testBlockWatch() {
        /* Test Depend on the remove rpc server
        val web3j = Web3j.build(HttpService("http://39.96.66.145:8545"))
        val blockReq = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(10)), true).send()
        println("The block info is ${blockReq.block.nonce}, ${blockReq.block.transactions.size}")
        */
    }
}